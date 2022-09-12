package com.services.sf.mongodb;

import com.mongodb.bulk.BulkWriteInsert;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.model.*;
import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.base.BulkResponse;
import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.common.domain.util.LocalContext;
import com.services.common.exception.NotImplementedException;
import com.services.sf.commons.base.BaseRepositoryImpl;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public class BaseMongoRepositoryImpl<T extends BaseMongoEntity, E extends BaseMongoDomain>
        extends BaseRepositoryImpl<T, E, ObjectId>
        implements BaseMongoRepository<T, E> {

    @Inject
    protected LocalContext localContext;

    protected CodecRegistry registry;
    protected Codec<Document> documentCodec;
    protected BaseMongoMapper<T, E> mapper;
    protected Boolean hasTenant;

    public BaseMongoRepositoryImpl(BaseMongoMapper<T, E> mapper) {
        this.registry = this.mongoCollection().getCodecRegistry();
        this.documentCodec = registry.get(Document.class);
        this.mapper = mapper;
        this.hasTenant = true;
    }

    public BaseMongoRepositoryImpl(BaseMongoMapper<T, E> mapper, @NotNull Boolean hasTenant) {
        this(mapper);
        this.hasTenant = hasTenant;
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

    public Uni<E> create(E e) {
        return this.create(e, null);
    }

    public Uni<E> get(String id, ClientSession clientSession) {
        Document query = QueryBuilder.builder()
                .id(id)
                .build();

        return this.findOne(query, clientSession);
    }

    public Uni<E> create(E e, ClientSession clientSession) {

        T t = mapper.toFirst(e);

        Instant now = Instant.now();
        setCreateFields(t);
        return (clientSession == null ? this.persist(t) :
                this.mongoCollection().insertOne(clientSession, t)
                        .map(insertOneResult -> {
                            if (insertOneResult.getInsertedId() != null)
                                t.setId(insertOneResult.getInsertedId().asObjectId().getValue());
                            return t;
                        })
        ).map(mapper::toSecond);
    }

    public Uni<E> upsert(E e, E filter) {
        return this.upsert(e, filter, null);
    }

    public Uni<E> upsert(E e, E filter, ClientSession clientSession) {
        Instant now = Instant.now();

        T t = mapper.toFirst(e);

        FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(true);


        setUpdateFields(t);

        Document enhancedFilter = enhanceFilter(toDocument(filter));

        Document enhancedUpdate = enhanceUpdate(QueryBuilder.builder()
                .set(toDocument(t))
                .inc("version", 1)
                .setOnInsert("createdAt", now)
                .setOnInsert("createdBy", localContext.getCallerService())
                .build());

        return (clientSession == null ?
                this.mongoCollection().findOneAndUpdate(enhancedFilter, enhancedUpdate, findOneAndUpdateOptions) :
                this.mongoCollection().findOneAndUpdate(clientSession, enhancedFilter, enhancedUpdate, findOneAndUpdateOptions)
        ).map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<E> get(@SpanAttribute("query.id") String id) {
        return this.get(id, null);
    }

    public Multi<E> getByIds(List<String> ids, List<String> sortOrder) {
        return this.getByIds(ids, sortOrder, null);
    }


    public Multi<E> getByIds(List<String> ids, List<String> sortOrder, ClientSession clientSession) {

        Document query = QueryBuilder.builder()
                .ids(ids)
                .build();

        FindOptions findOptions = new FindOptions()
                .sort(getSortDocument(sortOrder));

        return this.findAll(query, findOptions, clientSession);
    }

    public Uni<E> patch(E e) {

        return this.patch(e, null);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<E> patch(E e, ClientSession clientSession) {

        T t = mapper.toFirst(e);

        setUpdateFields(t);

        FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(false);

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .id(t.getId())
                .build());

        Document enhancedUpdate = enhanceUpdate(QueryBuilder.builder()
                .set(toDocument(t))
                .build());

        return (clientSession == null ?
                this.mongoCollection().findOneAndUpdate(enhancedFilter, enhancedUpdate, findOneAndUpdateOptions) :
                this.mongoCollection().findOneAndUpdate(clientSession, enhancedFilter, enhancedUpdate, findOneAndUpdateOptions)
        )
                .map(mapper::toSecond);
    }

    public Uni<E> put(E e) {

        return this.put(e, null);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<E> put(E e, ClientSession clientSession) {

        T t = mapper.toFirst(e);

        setCreateFields(t);

        FindOneAndReplaceOptions findOneAndReplaceOptions = new FindOneAndReplaceOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(false);

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder().id(t.getId()).build());

        return (clientSession == null ?
                this.mongoCollection().findOneAndReplace(enhancedFilter, t, findOneAndReplaceOptions) :
                this.mongoCollection().findOneAndReplace(clientSession, enhancedFilter, t, findOneAndReplaceOptions)
        ).map(mapper::toSecond);
    }

    public Multi<String> bulkCreate(List<E> eList) {

        return this.bulkCreate(eList, null);

    }

    @Override
    public Uni<List<E>> bulkCreateWithResponse(List<E> es) {
        return Uni.createFrom().failure(new NotImplementedException("bulkCreateWithResponse not implemented yet for mongo"));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<String> bulkCreate(List<E> eList, ClientSession clientSession) {

        Instant now = Instant.now();

        List<T> tList = mapper.toFirst(eList);

        tList.forEach(this::setCreateFields);

        return ((clientSession == null ?
                this.mongoCollection().insertMany(tList) :
                this.mongoCollection().insertMany(clientSession, tList))
                .onItem().transformToMulti(insertManyResult ->
                        Multi.createFrom().iterable(insertManyResult
                                        .getInsertedIds()
                                        .values())
                                .map(bsonValue -> bsonValue.asObjectId().getValue())
                ))
                .map(ObjectId::toString);
    }

    public Uni<Void> bulkPatch(List<E> eList) {
        return this.bulkPatch(eList, null).replaceWithVoid();
    }

    public Uni<Void> bulkPatch(List<E> eList, ClientSession clientSession) {

        List<T> tList = mapper.toFirst(eList);

        BulkWriteOptions bulkWriteOptions = new BulkWriteOptions()
                .ordered(false);

        List<WriteModel<T>> updateList = new ArrayList<>();

        tList.forEach(t -> {

            setUpdateFields(t);

            Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                    .id(t.getId())
                    .build());

            Document enhancedUpdate = enhanceUpdate(QueryBuilder.builder()
                    .set(toDocument(t))
                    .build());

            updateList.add(new UpdateOneModel<T>(enhancedFilter, enhancedUpdate));
        });

        return clientSession == null ?
                this.mongoCollection().bulkWrite(updateList, bulkWriteOptions).replaceWithVoid() :
                this.mongoCollection().bulkWrite(clientSession, updateList, bulkWriteOptions).replaceWithVoid();
    }

    public Uni<BulkResponse> bulkWrite(List<UpdateManyModel<E>> eList, ClientSession clientSession) {

        List<UpdateManyModel<T>> updateList = new ArrayList<>();

        BulkWriteOptions bulkWriteOptions = new BulkWriteOptions().ordered(false);

        for (UpdateManyModel<E> updateManyModel : eList) {
            Document enhancedFilter = enhanceFilter(documentCodec.decode(updateManyModel.getFilter().toBsonDocument().asBsonReader(), DecoderContext.builder().build()));
            Document enhancedUpdate = enhanceUpdate(documentCodec.decode(updateManyModel.getUpdate().toBsonDocument().asBsonReader(), DecoderContext.builder().build()));

            updateList.add(new UpdateManyModel<>(enhancedFilter, enhancedUpdate, updateManyModel.getOptions()));
        }

        return (clientSession == null ?
                this.mongoCollection().bulkWrite(updateList, bulkWriteOptions) :
                this.mongoCollection().bulkWrite(clientSession, updateList, bulkWriteOptions))
                .map(bulkWriteResult -> BulkResponse.builder()
                        .insertIds(bulkWriteResult
                                .getInserts()
                                .stream()
                                .map(BulkWriteInsert::getId)
                                .map(bsonValue -> bsonValue.asObjectId().getValue().toString())
                                .collect(Collectors.toList()))
                        .upsertIds(bulkWriteResult
                                .getUpserts()
                                .stream()
                                .map(BulkWriteUpsert::getId)
                                .map(bsonValue -> bsonValue.asObjectId().getValue().toString())
                                .collect(Collectors.toList()))
                        .build());
    }

    public Uni<BulkResponse> bulkUpsert(List<E> eList) {
        return this.bulkUpsert(eList, null);
    }

    public Uni<BulkResponse> bulkUpsert(List<E> eList, ClientSession clientSession) {
        Instant now = Instant.now();

        List<T> tList = mapper.toFirst(eList);

        BulkWriteOptions bulkWriteOptions = new BulkWriteOptions()
                .ordered(false);

        List<WriteModel<T>> updateList = new ArrayList<>();

        tList.forEach(t -> {

            setUpdateFields(t);

            Document update = QueryBuilder.builder()
                    .set(toDocument(t))
                    .incVersion()
                    .setOnInsert("createdAt", now)
                    .setOnInsert("createdBy", localContext.getCallerService())
                    .build();

            Document filter = new Document();

            if (t.getId() != null) {

                filter = QueryBuilder.builder()
                        .id(t.getId())
                        .build();
            }

            updateList.add(new UpdateOneModel<T>(filter, update, new UpdateOptions().upsert(true)));
        });

        return (clientSession == null ?
                this.mongoCollection().bulkWrite(updateList, bulkWriteOptions) :
                this.mongoCollection().bulkWrite(clientSession, updateList, bulkWriteOptions))
                .map(bulkWriteResult -> BulkResponse.builder()
                        .insertIds(bulkWriteResult
                                .getInserts()
                                .stream()
                                .map(BulkWriteInsert::getId)
                                .map(bsonValue -> bsonValue.asObjectId().getValue().toString())
                                .collect(Collectors.toList()))
                        .upsertIds(bulkWriteResult
                                .getUpserts()
                                .stream()
                                .map(BulkWriteUpsert::getId)
                                .map(bsonValue -> bsonValue.asObjectId().getValue().toString())
                                .collect(Collectors.toList()))
                        .build())
                ;
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<E> delete(E e, ClientSession clientSession) {
        e.setDeletedAt(Instant.now());
        return this.patch(e, clientSession);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByPage(Integer offset,
                               Integer limit,
                               List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder().build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByCreatedAtGreaterThan(Instant t1,
                                               Integer offset,
                                               Integer limit,
                                               List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .gte("createdAt", t1)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }


    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByCreatedAtLessThan(Instant t1,
                                            Integer offset,
                                            Integer limit,
                                            List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .lte("createdAt", t1)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByCreatedAtBetween(Instant t1,
                                           Instant t2,
                                           Integer offset,
                                           Integer limit,
                                           List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .between("createdAt", t1, t2)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByLastModifiedAtGreaterThan(Instant t1,
                                                    Integer offset,
                                                    Integer limit,
                                                    List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .gte("lastModifiedAt", t1)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByLastModifiedAtLessThan(Instant t1,
                                                 Integer offset,
                                                 Integer limit,
                                                 List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .lte("lastModifiedAt", t1)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByLastModifiedAtBetween(Instant t1,
                                                Instant t2,
                                                Integer offset,
                                                Integer limit,
                                                List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .between("lastModifiedAt", t1, t2)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByLastDeletedAtGreaterThan(Instant t1,
                                                   Integer offset,
                                                   Integer limit,
                                                   List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .gte("deletedAt", t1)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByDeletedAtLessThan(Instant t1,
                                            Integer offset,
                                            Integer limit,
                                            List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .lte("deletedAt", t1)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findByDeletedAtBetween(Instant t1,
                                           Instant t2,
                                           Integer offset,
                                           Integer limit,
                                           List<String> sortOrder) {

        Document enhancedFilter = enhanceFilter(QueryBuilder.builder()
                .between("deletedAt", t1, t2)
                .build());

        return this.find(enhancedFilter, getSortDocument(sortOrder))
                .range(offset, offset + limit - 1)
                .stream()
                .map(mapper::toSecond);
    }

    public Uni<E> findOne(Document filter) {

        return this.findOne(filter, null);
    }

    public Uni<E> findOne(Document filter, ClientSession clientSession) {

        return this.findOne(filter, new FindOptions().limit(1), clientSession);

    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<E> findOne(@SpanAttribute("query.filter") Document filter, FindOptions findOptions, ClientSession clientSession) {

        Document enhancedFilter = enhanceFilter(filter);

        FindOptions enhancedFindOptions = ObjectUtils.isEmpty(findOptions) ?
                new FindOptions().limit(1) :
                findOptions.limit(1);

        return (clientSession == null ?
                this.mongoCollection().find(enhancedFilter, enhancedFindOptions) :
                this.mongoCollection().find(clientSession, enhancedFilter, enhancedFindOptions))
                .toUni()
                .map(mapper::toSecond);
    }

    public Multi<E> findAll(Document filter) {

        return this.findAll(filter, new FindOptions());
    }


    public Multi<E> findAll(Document filter, List<String> sortOrder, Integer
            offset, Integer limit) {

        FindOptions findOptions = new FindOptions()
                .skip(offset)
                .limit(limit)
                .sort(getSortDocument(sortOrder));

        return this.findAll(filter, findOptions);
    }

    public Multi<E> findAll(Document filter, FindOptions findOptions) {

        return this.findAll(filter, findOptions, null);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findAll(@SpanAttribute("query.filter") Document filter, FindOptions findOptions, ClientSession clientSession) {

        Document enhancedFilter = enhanceFilter(filter);

        return (clientSession == null ?
                this.mongoCollection().find(enhancedFilter, findOptions) :
                this.mongoCollection().find(clientSession, enhancedFilter, findOptions))
                .map(mapper::toSecond);
    }

    public Uni<Boolean> updateOne(@SpanAttribute("query.filter") Document filter, @SpanAttribute("query.update") Document update, ClientSession clientSession) {

        return this.updateOne(filter, update, new UpdateOptions(), clientSession);
    }

    public Uni<Boolean> updateOne(@SpanAttribute("query.filter") Document filter, @SpanAttribute("query.update") Document update, UpdateOptions updateOptions, ClientSession clientSession) {

        Document enhancedFilter = enhanceFilter(filter);

        Document enhancedUpdate = enhanceUpdate(update);

        return (clientSession == null ?
                this.mongoCollection().updateOne(enhancedFilter, enhancedUpdate, updateOptions) :
                this.mongoCollection().updateOne(clientSession, enhancedFilter, enhancedUpdate, updateOptions))
                .map(updateResult -> updateResult.getModifiedCount() > 0);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<List<E>> getList(@SpanAttribute("query.filter") Document filter) {

        Document enhancedFilter = enhanceFilter(filter);

        return this.list(enhancedFilter).map(mapper::toSecond);
    }

    public Uni<E> findOneAndUpdate(@SpanAttribute("query.filter") Document filter,
                                   @SpanAttribute("query.update") Document update,
                                   FindOneAndUpdateOptions options,
                                   ClientSession clientSession) {

        Document enhancedFilter = enhanceFilter(filter);

        Document enhancedUpdate = enhanceUpdate(update);

        return (clientSession == null ?
                this.mongoCollection().findOneAndUpdate(enhancedFilter, enhancedUpdate, options) :
                this.mongoCollection().findOneAndUpdate(clientSession, enhancedFilter, enhancedUpdate, options)
        ).map(mapper::toSecond);
    }

    public Uni<E> findOneAndReplace(@SpanAttribute("query.filter") Document filter,
                                    @SpanAttribute("query.replace") E replace,
                                    FindOneAndReplaceOptions options,
                                    ClientSession clientSession) {

        Document enhancedFilter = enhanceFilter(filter);

        T t = mapper.toFirst(replace);

        return (clientSession == null ?
                this.mongoCollection().findOneAndReplace(enhancedFilter, t, options) :
                this.mongoCollection().findOneAndReplace(clientSession, enhancedFilter, t, options)
        ).map(mapper::toSecond);
    }


    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Long> countDocuments(@SpanAttribute("query.filter") Document filter) {

        Document enhancedFilter = enhanceFilter(filter);

        return this.count(enhancedFilter);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> aggregate(@SpanAttribute("query.filter") List<Document> filters, ClientSession clientSession) {

        return (ObjectUtils.isEmpty(clientSession) ?
                this.mongoCollection().aggregate(filters) :
                this.mongoCollection().aggregate(clientSession, filters))
                .map(mapper::toSecond);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public <D> Multi<D> aggregate(@SpanAttribute("query.filter") List<Document> filters, Class<D> clazz, ClientSession clientSession) {

        return (ObjectUtils.isEmpty(clientSession) ?
                this.mongoCollection().aggregate(filters, clazz) :
                this.mongoCollection().aggregate(clientSession, filters, clazz));
    }


    public Sort getSort(List<String> sortOrder) {

        if (!ObjectUtils.isEmpty(sortOrder)) {
            Sort sort = sortOrder.get(0).startsWith("-") ?
                    Sort.by(sortOrder.get(0).substring(1), Sort.Direction.Descending)
                    : Sort.by(sortOrder.get(0), Sort.Direction.Ascending);

            for (int i = 1; i < sortOrder.size(); i++) {
                sort = sortOrder.get(i).startsWith("-") ?
                        sort.and(sortOrder.get(i).substring(1), Sort.Direction.Descending)
                        : sort.and(sortOrder.get(i), Sort.Direction.Ascending);
            }
            return sort;
        }
        return null;
    }


    public Document getSortDocument(List<String> sortOrder) {

        Document order = new Document();

        if (sortOrder != null && !sortOrder.isEmpty()) {

            for (String s : sortOrder) {
                if (s.startsWith("-")) {
                    order.append(s.substring(1), -1);
                } else {
                    order.append(s, 1);
                }
            }
        }

        return order;
    }

    protected BsonDocument toBsonDocument(T t) {
        BsonDocument document = new BsonDocument();
        Codec<T> codec = this.mongoCollection().getCodecRegistry().get((Class<T>) t.getClass());
        codec.encode(new BsonDocumentWriter(document), t, EncoderContext.builder().build());
        return document;
    }

    protected Document toDocument(T t) {
        return documentCodec.decode(toBsonDocument(t).asBsonReader(), DecoderContext.builder().build());
    }

    protected BsonDocument toBsonDocument(E e) {
        T t = mapper.toFirst(e);
        BsonDocument document = new BsonDocument();
        Codec<T> codec = this.mongoCollection().getCodecRegistry().get((Class<T>) t.getClass());
        codec.encode(new BsonDocumentWriter(document), t, EncoderContext.builder().build());
        return document;
    }

    protected Document toDocument(E e) {
        T t = mapper.toFirst(e);
        return documentCodec.decode(toBsonDocument(t).asBsonReader(), DecoderContext.builder().build());
    }

    protected Document enhanceFilter(Document filter) {
        return this.hasTenant ?
                QueryBuilder.builder(filter)
                        .tenant(localContext.getTenantId())
                        .isNotDeleted()
                        .build() :
                QueryBuilder.builder(filter)
                        .isNotDeleted()
                        .build();
    }

    protected Document enhanceUpdate(Document update) {
        if (ObjectUtils.isEmpty(update))
            return new Document("$inc", new Document("version", 1))
                    .append("$set", new Document("lastModifiedAt", Instant.now()));

        if (update.containsKey("$inc"))
            update.get("$inc", Document.class).put("version", 1);
        else
            update.put("$inc", new Document("version", 1));

        if (update.containsKey("$set")) {
            update.get("$set", Document.class).put("lastModifiedAt", Instant.now());
            update.get("$set", Document.class).remove("version");
        } else
            update.put("$set", new Document("lastModifiedAt", Instant.now()));
        return update;
    }

    protected static class QueryBuilder {

        private final Document document;

        private QueryBuilder() {
            this.document = new Document();
        }

        private QueryBuilder(Document document) {
            this.document = document;
        }

        public static QueryBuilder builder() {
            return new QueryBuilder();
        }

        public static QueryBuilder builder(Document document) {
            if (ObjectUtils.isEmpty(document))
                return new QueryBuilder();
            return new QueryBuilder(document);
        }

        public QueryBuilder tenant(String tenant) {
            this.document.append("tenantId", tenant);
            return this;
        }


        public QueryBuilder incVersion() {
            this.document.append("$inc", new Document("version", 1));
            return this;
        }

        public <K> QueryBuilder append(String key, K value) {
            this.document.append(key, value);
            return this;
        }

        public <K> QueryBuilder between(String field, K value1, K value2) {
            this.document.append(field, new Document("$gte", value1).append("$lte", value2));
            return this;
        }

        public QueryBuilder id(ObjectId id) {
            this.document.append("_id", id);
            return this;
        }

        public QueryBuilder id(String id) {
            this.document.append("_id", new ObjectId(id));
            return this;
        }


        public QueryBuilder ids(List<String> ids) {
            this.document.append("_id", new Document("$in", ids.stream().map(ObjectId::new).collect(Collectors.toList())));
            return this;
        }

        public QueryBuilder isNotDeleted() {
            this.document.append("deletedAt", null);
            return this;
        }

        public <K> QueryBuilder eq(String key, K value) {
            this.document.append(key, new Document("$eq", value));
            return this;
        }

        public <K> QueryBuilder ne(String key, K value) {
            this.document.append(key, new Document("$ne", value));
            return this;
        }

        public <K extends Bson> QueryBuilder inc(final K document) {
            this.document.append("$inc", document);
            return this;
        }

        public QueryBuilder inc(String key, Number value) {
            if (this.document.containsKey("$inc"))
                this.document.get("$inc", Document.class).put(key, value);
            else
                this.document.append("$inc", new Document(key, value));
            return this;
        }

        public <K extends Bson> QueryBuilder set(final K document) {
            this.document.append("$set", document);
            return this;
        }

        public QueryBuilder set(String key, Object value) {
            if (this.document.containsKey("$set"))
                this.document.get("$set", Document.class).put(key, value);
            else
                this.document.append("$set", new Document(key, value));
            return this;
        }

        public <K> QueryBuilder gte(K value) {
            this.document.append("$gte", value);
            return this;
        }

        public <K> QueryBuilder gte(String field, K value) {
            this.document.append(field, new Document("$gte", value));
            return this;
        }

        public <K> QueryBuilder lte(K value) {
            this.document.append("$lte", value);
            return this;
        }

        public <K> QueryBuilder lte(String field, K value) {
            this.document.append(field, new Document("$lte", value));
            return this;
        }

        public <K> QueryBuilder gt(K value) {
            this.document.append("$gt", value);
            return this;
        }

        public <K> QueryBuilder gt(String field, K value) {
            this.document.append(field, new Document("$gt", value));
            return this;
        }

        public <K> QueryBuilder lt(K value) {
            this.document.append("$lt", value);
            return this;
        }

        public <K> QueryBuilder lt(String field, K value) {
            this.document.append(field, new Document("$lt", value));
            return this;
        }

        public <K extends Bson> QueryBuilder setOnInsert(K value) {
            this.document.append("$setOnInsert", value);
            return this;
        }

        public <K> QueryBuilder setOnInsert(String field, K value) {
            if (this.document.containsKey("$setOnInsert"))
                this.document.get("$setOnInsert", Document.class).put(field, value);
            else
                this.document.append("$setOnInsert", new Document(field, value));
            return this;
        }

        public <K> QueryBuilder where(K function) {
            this.document.append("$where", function);
            return this;
        }

        public QueryBuilder exists(Boolean value) {
            this.document.append("$exists", value);
            return this;
        }

        public QueryBuilder exists(String field, Boolean value) {
            this.document.append(field, new Document("$exists", value));
            return this;
        }

        public <K> QueryBuilder in(List<K> values) {
            this.document.append("$in", values);
            return this;
        }

        public <K> QueryBuilder in(String field, List<K> values) {
            this.document.append(field, new Document("$in", values));
            return this;
        }

        public <K> QueryBuilder nin(List<K> values) {
            this.document.append("$nin", values);
            return this;
        }

        public <K> QueryBuilder nin(String field, List<K> values) {
            this.document.append(field, new Document("$nin", values));
            return this;
        }

        public QueryBuilder and(Document... documents) {
            this.document.append("$and", List.of(documents));
            return this;
        }

        public QueryBuilder unwind(Document document) {
            this.document.append("$unwind", document);
            return this;
        }

        public QueryBuilder unwind(String path) {
            this.document.append("$unwind", path);
            return this;
        }

        public QueryBuilder and(List<Document> documents) {
            this.document.append("$and", documents);
            return this;
        }

        public QueryBuilder sum(List<Document> documents) {
            this.document.append("$sum", documents);
            return this;
        }

        public QueryBuilder sum(Document... documents) {
            return this.sum(List.of(documents));
        }

        public QueryBuilder sum(Document document) {
            this.document.append("$sum", document);
            return this;
        }

        public QueryBuilder sum(String field) {
            this.document.append("$sum", field);
            return this;
        }

        public QueryBuilder sum(Number number) {
            this.document.append("$sum", number);
            return this;
        }

        public QueryBuilder project(Document document) {
            this.document.append("$project", document);
            return this;
        }

        public QueryBuilder avg(List<Document> documents) {
            this.document.append("$avg", documents);
            return this;
        }

        public QueryBuilder avg(Document... documents) {
            return this.avg(List.of(documents));
        }

        public QueryBuilder avg(Document document) {
            this.document.append("$avg", document);
            return this;
        }

        public QueryBuilder avg(String field) {
            this.document.append("$avg", field);
            return this;
        }

        public <K extends Bson> QueryBuilder not(K document) {
            this.document.append("$not", document);
            return this;
        }

        public <K extends Bson> QueryBuilder not(String field, K document) {
            this.document.append(field, new Document("$not", document));
            return this;
        }

        public <K extends Bson> QueryBuilder expr(K document) {
            this.document.append("$expr", document);
            return this;
        }

        public <K extends Bson> QueryBuilder or(K... documents) {
            this.document.append("$or", List.of(documents));
            return this;
        }

        public <K extends Bson> QueryBuilder or(List<K> documents) {
            this.document.append("$or", documents);
            return this;
        }

        public <K extends Bson> QueryBuilder match(K document) {
            this.document.append("$match", document);
            return this;
        }

        public <K> QueryBuilder match(String field, K value) {
            if (this.document.containsKey("$match"))
                this.document.get("$match", Document.class).put(field, value);
            else
                this.document.append("$match", new Document(field, value));
            return this;
        }

        public <K extends Bson> QueryBuilder sort(K document) {
            this.document.append("$sort", document);
            return this;
        }

        public <K> QueryBuilder sort(String field, Integer value) {
            if (value != 1 && value != -1)
                value = 1;
            if (this.document.containsKey("$sort"))
                this.document.get("$sort", Document.class).put(field, value);
            else
                this.document.append("$sort", new Document(field, value));
            return this;
        }

        public <K extends Bson> QueryBuilder group(K document) {
            this.document.append("$group", document);
            return this;
        }

        public <K extends Bson> QueryBuilder replaceRoot(K document) {
            this.document.append("$replaceRoot", document);
            return this;
        }

        public <K> QueryBuilder first(K key) {
            this.document.append("$first", key);
            return this;
        }

        public Document build() {
            if (this.document.containsKey("$set")) {

                Document set = this.document.get("$set", Document.class);

                if (this.document.containsKey("$setOnInsert")) {

                    Set<String> setOnInsertKeySet = this.document.get("$setOnInsert", Document.class).keySet();

                    for (String key : setOnInsertKeySet)
                        set.remove(key);
                }

                if (this.document.containsKey("$inc")) {
                    Set<String> incKeySet = this.document.get("$inc", Document.class).keySet();
                    for (String key : incKeySet)
                        set.remove(key);
                }

                this.document.put("$set", set);
            }
            return new Document(this.document);
        }

    }
}
