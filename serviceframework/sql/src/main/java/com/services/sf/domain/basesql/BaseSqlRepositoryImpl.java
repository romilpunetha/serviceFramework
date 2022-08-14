package com.services.sf.domain.basesql;

import com.google.common.collect.Sets;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.HQLTemplates;
import com.querydsl.jpa.JPQLSerializer;
import com.querydsl.jpa.impl.JPAQuery;
import com.services.common.domain.basemongo.BulkResponse;
import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.common.domain.util.LocalContext;
import com.services.sf.domain.base.BaseRepositoryImpl;
import com.services.sf.domain.basesql.util.QueryUtil;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;

import javax.annotation.Nullable;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
@IfBuildProperty(name = "framework.sql.enabled", stringValue = "true")
public class BaseSqlRepositoryImpl<T extends BaseSqlEntity, E extends BaseSqlDomain, QE extends EntityPathBase<T>>
        extends BaseRepositoryImpl<T, E, Long>
        implements BaseSqlRepository<T, E> {

    private final Set<String> allowedSortFields = Stream.<Path<?>>of(
            QBaseSqlEntity.baseSqlEntity.id,
            QBaseSqlEntity.baseSqlEntity.createdAt,
            QBaseSqlEntity.baseSqlEntity.lastModifiedAt
    ).map(field -> field.getMetadata().getName()).collect(Collectors.toUnmodifiableSet());
    protected BaseSqlMapper<T, E> mapper;
    protected Boolean hasTenant;
    @Inject
    protected LocalContext localContext;
    @Inject
    protected QueryUtil queryUtil;
    private Class<QE> qEntityClass;
    @Getter
    private Class<T> entityClass;
    @Getter
    private String entityName;


    public BaseSqlRepositoryImpl(BaseSqlMapper<T, E> mapper, Class<T> clazz, Class<QE> qEntityClass, @NotNull Boolean hasTenant) {
        this.mapper = mapper;
        this.qEntityClass = qEntityClass;
        this.hasTenant = hasTenant;
        this.entityClass = clazz;
        this.entityName = clazz.getSimpleName();
    }

    public BaseSqlRepositoryImpl(BaseSqlMapper<T, E> mapper, Class<T> clazz, Class<QE> qEntityClass) {
        this(mapper, clazz, qEntityClass, true);
    }

    @Override
    public Uni<E> create(E e) {
        var t = mapper.toFirst(e);
        setCreateFields(t);
        return this.doPersist(t).map(mapper::toSecond);
    }

    protected void setCreateFields(T t) {
        Instant now = Instant.now();
        t.setCreatedAt(now);
        t.setCreatedBy(localContext.getCallerService());
        t.setLastModifiedAt(now);
        t.setTenantId(localContext.getTenantId());
        t.setIsTestData(localContext.isTestData());
        t.setVersion(1L);
    }

    protected void setUpdateFields(T t) {
        Instant now = Instant.now();
        t.setLastModifiedAt(now);
        t.setTenantId(localContext.getTenantId());
        t.setIsTestData(localContext.isTestData());
    }

    // TODO check if flush is needed, as there persistence problems locally without flush
    protected Uni<T> doPersist(T t) {
        return persistAndFlush(t);
    }

    protected Multi<T> doPersist(Collection<T> entities) {
        return persist(entities)
                // flush is needed to get back the persisted ids from db
                .chain(this::flush)
                .onItem().transformToMulti(a -> Multi.createFrom().iterable(entities));
    }

    @Override
    public Uni<E> upsert(E e, E filter) {
        if (Objects.isNull(filter.getId())) {
            return this.create(e);
        } else {
            return this.getById(Long.parseLong(filter.getId()))
                    .onItem().ifNotNull().transformToUni((foundEntity) -> {
                        T newEntity = patchExistingEntity(foundEntity, e);
                        return doPersist(newEntity).map(mapper::toSecond);
                    })
                    .onItem().ifNull().switchTo(() -> create(e));
        }
    }

    @Override
    public Uni<E> get(String id) {
        return this.getById(Long.parseLong(id)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> getByIds(List<String> ids, List<String> sortOrder) {
        var query = newQuery()
                .where(baseEntity().id.in(ids.stream().map(Long::parseLong).toList()))
                .orderBy(sortOrder).build();
        return this.findMany(query, Limits.none()).map(mapper::toSecond);
    }

    /**
     * This is needed for getting attached entities which we want to re-use
     *
     * @param ids
     * @param sortOrder
     * @return
     */
    protected Multi<T> getEntitiesByIds(List<String> ids, List<String> sortOrder) {
        var query = newQuery()
                .where(baseEntity().id.in(ids.stream().map(Long::parseLong).toList()))
                .orderBy(sortOrder).build();
        return this.findMany(query, Limits.none());
    }

    protected T patchExistingEntity(T oldEntity, E newDomain) {
        Long oldVersion = oldEntity.getVersion();
        String newDomainId = newDomain.getId();
        newDomain.setId(null);
        mapper.toFirst(oldEntity, newDomain);
        newDomain.setId(newDomainId);
        setUpdateFields(oldEntity);
        oldEntity.setVersion(oldVersion + 1);
        return oldEntity;
    }

    protected T constructPatchedEntity(T oldEntity, E newDomain) {
        // create a copy of old entity in memory
        var newEntity = mapper.toFirst(mapper.toSecond(oldEntity));
        // TODO check if this works
        var oldVersion = newEntity.getVersion();
        // update in-memory entity copy with new data
        mapper.toFirst(newEntity, newDomain);
        var now = Instant.now();
        newEntity.setLastModifiedAt(now);
        newEntity.setVersion(oldVersion + 1);
        return newEntity;
    }

    @Override
    public Uni<E> patch(E e) {
        return this.getById(Long.parseLong(e.getId())).onItem().ifNotNull().transformToUni(entity -> {
            //TODO confirm that all jpa entities are managed.
            var newEntity = patchExistingEntity(entity, e);
            return doPersist(newEntity);
        }).map(mapper::toSecond);
    }

    /**
     * This method is causing call to internal findByID, so DO NOT USE THIS.
     *
     * @param id
     * @return
     */
    @Override
    @Deprecated
    public Uni<T> findById(Long id) {
        return getById(id);
    }

    @Override
    public Uni<T> getById(Long id) {
        var query = newQuery()
                .where(baseEntity().id.eq(id))
                .build();
        return this.findOne(query);
    }

    @Override
    public Set<String> getAllowedSortFields() {
        return allowedSortFields;
    }

    private QBaseSqlEntity baseEntity() {
        return entityOf(BaseSqlEntity.class, QBaseSqlEntity.class);
    }

    protected QE currentEntity() {
        return entityOf(this.entityClass, this.qEntityClass);
    }

    protected <A> A entityOf(Class<?> entityClass, Class<?> qEntityClass) {
        return (A) createEntity((Class<T>) entityClass, (Class<QE>) qEntityClass).as((Class<QE>) qEntityClass);
    }

    private QE createEntity(Class<T> cls, Class<QE> qEntityClass) {
        return new PathBuilder<>(cls, PathMetadataFactory.forVariable(StringUtils.uncapitalize(getEntityName()))).as(qEntityClass);
    }

    @Override
    public Uni<E> put(E e) {
        return this.getById(Long.parseLong(e.getId())).onItem().ifNotNull().transformToUni(entity -> {
            var now = Instant.now();
            var oldCreatedAt = entity.getCreatedAt();
            var oldDeletedAt = entity.getDeletedAt();
            var tenantId = entity.getTenantId();
            var id = entity.getId();
            var isTestData = entity.getIsTestData();
            // TODO this needs to be fixed as currently we have no way of nullifying existing values
            patchExistingEntity(entity, e);
            // TODO in mongo repository we are also updating createdAt, is that a bug?
            entity.setCreatedAt(oldCreatedAt);
            entity.setDeletedAt(oldDeletedAt);
            entity.setTenantId(tenantId);
            entity.setIsTestData(isTestData);
            entity.setId(id);
            return this.doPersist(entity).map(mapper::toSecond);
        });
    }

    @Override
    public Multi<String> bulkCreate(List<E> es) {
        return bulkCreateWithResponseMulti(es).map(BaseSqlDomain::getId);
    }

    private Multi<E> bulkCreateWithResponseMulti(List<E> es) {
        List<T> tList = mapper.toFirst(es);
        tList.forEach(this::setCreateFields);
        return this.doPersist(tList)
                .map(mapper::toSecond);
    }


    @Override
    public Uni<List<E>> bulkCreateWithResponse(List<E> es) {
        return bulkCreateWithResponseMulti(es).collect().asList();
    }

    @Override
    //TODO do we need to check if count unique input ids == count of unique output ids when doing find?
    public Uni<Void> bulkPatch(List<E> es) {
        var idEntityMap = es.stream().map(e -> Map.entry(e.getId(), e))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> second));
        return this.getEntitiesByIds(idEntityMap.keySet().stream().toList(), Collections.emptyList())
                .map(e -> patchExistingEntity(e, idEntityMap.get(String.valueOf(e.getId()))))
                .collect().with(Collectors.toList())
                .onItem().transformToMulti(this::doPersist)
                .collect().asList()
                .replaceWithVoid();
    }

    @Override
    //TODO Need to return insertedIds
    public Uni<BulkResponse> bulkUpsert(List<E> es) {
        var createDomains = new ArrayList<E>();
        var upsertEntityMap = es.stream()
                .filter(e -> {
                    if (e.getId() == null) {
                        // all objects with no ids are to be created
                        createDomains.add(e);
                        return false;
                    }
                    return true;
                })
                .map(e -> Map.entry(e.getId(), e))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> second));
        var upsertIds = new HashSet<String>();
        return this.getEntitiesByIds(upsertEntityMap.keySet().stream().toList(), Collections.emptyList())
                .map(foundEntity -> {
                    var foundId = String.valueOf(foundEntity.getId());
                    upsertIds.add(foundId);
                    return patchExistingEntity(foundEntity, upsertEntityMap.get(foundId));
                })
                .collect().with(Collectors.toList())
                .map(patchedEntities -> {
                    var notFoundIds = Sets.difference(upsertEntityMap.keySet(), upsertIds);
                    createDomains.addAll(notFoundIds.stream().map(upsertEntityMap::get).toList());
                    var createEntities = createDomains.stream().map(mapper::toFirst).toList();
                    createEntities.forEach(this::setCreateFields);
                    return Stream.of(patchedEntities, createEntities)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());
                })
                .chain(entities -> doPersist(entities).collect().asList())
                // TODO fix ids here
                .map(entities -> {
                            var upsertIdsSet = new LinkedHashSet<>(upsertIds);
                            return BulkResponse.builder().upsertIds(upsertIdsSet.stream().toList())
                                    .insertIds(
                                            Sets.difference(
                                                    entities.stream().map(BaseSqlEntity::getId).collect(Collectors.toUnmodifiableSet()),
                                                    upsertIdsSet
                                            ).stream().map(String::valueOf).toList())
                                    .build();
                        }
                );
    }

    @Override
    public Uni<E> delete(E e) {
        e.setDeletedAt(Instant.now());
        return this.patch(e);
    }

    @Override
    public Multi<E> findByPage(Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByCreatedAtGreaterThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.GT, baseEntity().createdAt, Expressions.constant(t1)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByCreatedAtLessThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.LT, baseEntity().createdAt, Expressions.constant(t1)))
                .orderBy(sortOrder)
                .build();

        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByCreatedAtBetween(Instant t1, Instant t2, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.BETWEEN, baseEntity().createdAt, Expressions.constant(t1), Expressions.constant(t2)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByLastModifiedAtGreaterThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.GT, baseEntity().lastModifiedAt, Expressions.constant(t1)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByLastModifiedAtLessThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.LT, baseEntity().lastModifiedAt, Expressions.constant(t1)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByLastModifiedAtBetween(Instant t1, Instant t2, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.BETWEEN, baseEntity().lastModifiedAt, Expressions.constant(t1), Expressions.constant(t2)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByLastDeletedAtGreaterThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.GT, baseEntity().deletedAt, Expressions.constant(t1)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByDeletedAtLessThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.LT, baseEntity().deletedAt, Expressions.constant(t1)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    @Override
    public Multi<E> findByDeletedAtBetween(Instant t1, Instant t2, Integer offset, Integer limit, List<String> sortOrder) {
        var query = newQuery()
                .where(Expressions.predicate(Ops.BETWEEN, baseEntity().deletedAt, Expressions.constant(t1), Expressions.constant(t2)))
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

    protected Uni<T> findOne(HQLQuery<T> query) {
        return find(query.getFinalHql(), query.getBindings()).firstResult();
    }

    /**
     * Use this when returning multiple columns to wrap them in the projectedClass
     *
     * @param query
     * @param projectedClass
     * @param <R>
     * @return
     */
    protected <R> Uni<R> findOne(HQLQuery<T> query, Class<R> projectedClass) {
        return queryUtil.projectResultsToClass(findOne(query), projectedClass);
    }

    /**
     * Use this when selecting a single column would like to simply cast it to the resulting type
     *
     * @param query
     * @param projectedClass
     * @param <R>
     * @return
     */
    protected <R> Uni<R> findOneAs(HQLQuery<T> query, Class<R> projectedClass) {
        return findOne(query).map(a -> (R) a);
    }

    protected Multi<T> findMany(HQLQuery<T> query, Limits limits) {
        var internalQuery = find(query.getFinalHql(), query.getBindings());
        if (Objects.nonNull(limits)) {
            internalQuery = internalQuery.range(limits.getOffset(), limits.getOffset() + limits.getLimit() - 1);
        }
        return internalQuery.stream();
    }

    /**
     * Use this when returning multiple columns to wrap them in the projectedClass
     *
     * @param query
     * @param limits
     * @param projectedClass
     * @return
     */
    protected <R> Multi<R> findMany(HQLQuery<T> query, Limits limits, Class<R> projectedClass) {
        return queryUtil.projectResultsToClass(findMany(query, limits), projectedClass);
    }

    /**
     * Use this when selecting a single column would like to simply cast it to the resulting type
     *
     * @param query
     * @param <R>
     * @return
     */
    protected <R> Multi<R> findManyAs(HQLQuery<T> query, Limits limits) {
        return findMany(query, limits).map(a -> (R) a);
    }

    /**
     * This generates the enhanced filters only for the current entity/table as HQLQuery only supports that for now.
     *
     * @return filter on current table
     */
    protected Predicate enhancedPredicate() {
        return new BooleanBuilder();
    }

    /**
     * This method is for getting the enhanced condition/filter when using native queries
     *
     * @param tableName to generate the enhanced condition/filter for
     * @return filter on the specified table
     */
    protected Condition enhancedCondition(String tableName) {
        return (hasTenant ? DSL.field(DSL.name(tableName, "tenantId")).eq(localContext.getTenantId()) : DSL.noCondition())
                .and(DSL.field(DSL.name(tableName, "deletedAt")).isNull());
    }

    /**
     * Starts a query with the {@link #enhancedPredicate} for the current table and selecting the current table in progress.
     *
     * @return
     */
    protected HQLBuilder<T> newQuery() {
        return newQuery(true);
    }

    protected HQLBuilder<T> newQuery(boolean fromCurrentEntity) {
        if (fromCurrentEntity) {
            return new HQLBuilder<>(this::enhancedPredicate, new PathBuilder<T>(this.getEntityClass(), currentEntity().getMetadata()), this::globalEntityFilter);
        }
        return new HQLBuilder<>(null, null, this::globalEntityFilter);
    }

    protected NativeQuery.Builder nativeEnhancedSelect() {
        return nativeEnhancedSelect(this::enhancedCondition);
    }

    protected NativeQuery.Builder nativeEnhancedSelect(Function<String, Condition> enhancedCondition) {
        return new NativeQuery.Builder(DSL.using(SQLDialect.POSTGRES).selectQuery(), enhancedCondition);
    }

    protected Predicate globalEntityFilter(EntityPathBase<?> joinPath) {
        return Expressions.predicate(Ops.IS_NULL, ExpressionUtils.path(String.class, joinPath, "deletedAt"))
                .and(Expressions.predicate(Ops.EQ, ExpressionUtils.path(String.class, joinPath, "tenantId"), Expressions.constant(localContext.getTenantId()))
                );
    }

    protected TableLike<Record> tableName(String name) {
        return DSL.table(DSL.name(name));
    }

    protected Field<Object> nativeEntityField(String fieldName) {
        return DSL.field(DSL.name(getEntityName(), fieldName));
    }

    @Getter
    @AllArgsConstructor
    @Builder
    protected static final class Limits {
        private final @NotNull int offset;
        private final @NotNull int limit;

        public static Limits of(int offset, int limit) {
            return new Limits(offset, limit);
        }

        public static Limits none() {
            return null;
        }
    }

    @Getter
    @AllArgsConstructor
    @ToString
    protected final class HQLQuery<E> {
        private final String finalHql;
        private final JPAQuery<E> finalQuery;
        private final JPAQuery<E> initialQuery;
        private final JPAQuery<E> modifications;
        private final Object[] bindings;
        private final HQLBuilder<E> underlyingBuilder;
    }

    protected class HQLBuilder<Q> {


        private final JPAQuery<Q> query;

        private final @Nullable
        Supplier<Predicate> enhancedPredicate;
        private final @Nullable
        PathBuilder<Q> currentEntityPath;
        private final List<EntityPathBase<?>> joinedTables = new ArrayList<>();
        private final List<EntityPathBase<?>> fromTables = new ArrayList<>();
        private final Function<EntityPathBase<?>, Predicate> globalFilter;

        /**
         * @param currentEntityPredicate predicate to add to primary table always
         * @param currentEntityPath      primary table path in order to apply sortOrders etc.
         */
        public HQLBuilder(@Nullable Supplier<Predicate> currentEntityPredicate, @Nullable PathBuilder<Q> currentEntityPath, Function<EntityPathBase<?>, Predicate> globalFilter) {
            this.enhancedPredicate = currentEntityPredicate;
            this.currentEntityPath = currentEntityPath;
            this.query = new JPAQuery<>();
            if (Objects.nonNull(currentEntityPath)) {
                this.query.from(currentEntityPath);
                addFromTable(currentEntityPath);
            }
            this.globalFilter = globalFilter;

        }

        public static <A> String stringify(JPAQuery<A> query) {
            var querySerializer = new JPQLSerializer(HQLTemplates.DEFAULT);
            querySerializer.serialize(query.getMetadata(), false, null);
            return query.toString();
        }

        private void addJoinTable(EntityPathBase<?> path) {
            this.joinedTables.add(path);
        }

        private void addFromTable(EntityPathBase<?> path) {
            this.fromTables.add(path);
        }

        public HQLBuilder<Q> select(Expression<?>... expressions) {
            this.query.select(expressions);
            return this;
        }

        public HQLBuilder<Q> from(EntityPathBase<?>... args) {
            this.query.from(args);
            Arrays.stream(args).forEach(this::addFromTable);
            return this;
        }

        public HQLBuilder<Q> where(Predicate predicate) {
            this.query.where(predicate);
            return this;
        }

        public HQLBuilder<Q> orderBy(OrderSpecifier<?>... orderSpecifiers) {
            this.query.orderBy(orderSpecifiers);
            return this;
        }

        public <P> HQLBuilder<Q> leftJoin(EntityPathBase<P> entityPath) {
            this.query.leftJoin(entityPath);
            enhanceJoin(entityPath);
            addJoinTable(entityPath);
            return this;
        }

        public <P> HQLBuilder<Q> join(EntityPathBase<P> entityPath) {
            this.query.join(entityPath);
            enhanceJoin(entityPath);
            addJoinTable(entityPath);
            return this;
        }

        public <P> HQLBuilder<Q> innerJoin(EntityPathBase<P> entityPath) {
            this.query.innerJoin(entityPath);
            enhanceJoin(entityPath);
            addJoinTable(entityPath);
            return this;
        }

        public <P> HQLBuilder<Q> rightJoin(EntityPathBase<P> entityPath) {
            this.query.rightJoin(entityPath);
            enhanceJoin(entityPath);
            addJoinTable(entityPath);
            return this;
        }

        public HQLBuilder<Q> on(Predicate... conditions) {
            this.query.on(conditions);
            return this;
        }

        public HQLBuilder<Q> groupBy(Expression<?>... expressions) {
            this.query.groupBy(expressions);
            return this;
        }

        /**
         * Adds order-by for the primary table selected.
         *
         * @param sortOrders sortOrders for the primary table selected.
         * @return
         */
        public HQLBuilder<Q> orderBy(List<String> sortOrders) {
            if (Objects.isNull(this.currentEntityPath)) {
                throw new UnsupportedOperationException("Simple sortOrders not supported for query with primary table not defined.");
            }
            List<String> correctedOrders = Objects.isNull(sortOrders) ? Collections.emptyList() : sortOrders;
            return this.orderBy(correctedOrders.stream().map(order -> {
                Order orderType;
                PathBuilder<?> column;
                if (order.startsWith("-")) {
                    orderType = Order.DESC;
                    column = currentEntityPath.get(order.substring(1));
                } else {
                    orderType = Order.ASC;
                    column = currentEntityPath.get(order);
                }
                if (!getAllowedSortFields().contains(column.getMetadata().getName())) {
                    throw new IllegalArgumentException("Column not allowed for sorting: " + column);
                }
                return new OrderSpecifier(orderType, column);
            }).toList().toArray(new OrderSpecifier[0]));
        }

        public HQLQuery<Q> build() {
            if (Objects.nonNull(this.enhancedPredicate)) {
                this.query.getMetadata().addWhere(enhancedPredicate.get());
            }
            this.fromTables.stream().distinct().forEach(this::enhanceFrom);

            return new HQLQuery<Q>(stringify(query), query, query, new JPAQuery<>(), queryUtil.getBindings(query).toArray(), this);

        }

        private void enhanceJoin(EntityPathBase<?> joinPath) {
            enhanceJoin(this.query, joinPath);
        }

        private void enhanceFrom(EntityPathBase<?> joinPath) {
            enhanceFrom(this.query, joinPath);
        }

        private void enhanceJoin(JPAQuery<Q> query, EntityPathBase<?> joinPath) {
            if (Objects.nonNull(globalFilter)) {
                query.on(globalFilter.apply(joinPath));
            }
        }

        private void enhanceFrom(JPAQuery<Q> query, EntityPathBase<?> joinPath) {
            if (Objects.nonNull(globalFilter)) {
                query.where(globalFilter.apply(joinPath));
            }
        }
    }
}
