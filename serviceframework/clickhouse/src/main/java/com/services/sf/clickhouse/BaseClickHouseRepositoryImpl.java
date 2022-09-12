package com.services.sf.clickhouse;

import com.clickhouse.client.*;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.services.common.baseclickhouse.BaseClickhouseDomain;
import com.services.common.domain.base.BulkResponse;
import com.services.common.exception.NotImplementedException;
import com.services.sf.commons.base.BaseRepositoryImpl;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.clickhouse.enabled", stringValue = "true")
public class BaseClickHouseRepositoryImpl<T extends BaseClickHouseEntity<ID>, E extends BaseClickhouseDomain, ID>
        extends BaseRepositoryImpl<T, E, ID>
        implements BaseClickHouseRepository<T, E, ID> {

    final ObjectMapper objectMapper;
    protected ClickHouseRequest<?> request;
    BaseClickHouseMapper<T, E, ID> mapper;
    String tableName;
    Class<T> entityClass;
    ClickHouseClient client;
    @ConfigProperty(name = "clickhouse.hosts", defaultValue = "jdbc:ch:http://localhost:8123/my_db")
    String hosts;

    public BaseClickHouseRepositoryImpl() {
        Type genericSuperClass = getClass().getGenericSuperclass();

        ParameterizedType parametrizedType = null;
        while (parametrizedType == null) {
            if ((genericSuperClass instanceof ParameterizedType)) {
                parametrizedType = (ParameterizedType) genericSuperClass;
            } else {
                genericSuperClass = ((Class<?>) genericSuperClass).getGenericSuperclass();
            }
        }

        Type[] typeArgs = parametrizedType.getActualTypeArguments();
        if (typeArgs != null && typeArgs.length >= 1) {
            this.entityClass = (Class<T>) typeArgs[0];
            this.tableName = StringUtils.isNotBlank(entityClass.getAnnotation(ClickHouseEntity.class).name()) ?
                    entityClass.getAnnotation(ClickHouseEntity.class).name() :
                    entityClass.getSimpleName();
        }

        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public BaseClickHouseRepositoryImpl(BaseClickHouseMapper<T, E, ID> mapper) {
        this();
        this.mapper = mapper;
    }

    @PostConstruct
    public void set() {
        ClickHouseNodes servers = ClickHouseNodes.of(hosts);
        this.client = ClickHouseClient.newInstance(ClickHouseProtocol.HTTP);
        this.request = client.connect(servers).format(ClickHouseFormat.JSONEachRow);
    }

    @PreDestroy
    public void destroy() {
        this.client.close();
    }

    public Uni<E> create(E e) {
        throw new NotImplementedException();
    }

    public Uni<E> upsert(E e, E filter) {
        throw new NotImplementedException();
    }

    public Uni<E> get(String id) {
        return Uni.createFrom().completionStage(request.query("select * from " + this.tableName).execute())
                .map(Unchecked.function(response -> ObjectUtils.isEmpty(response.records()) ? null : mapper.toSecond(
                        this.objectMapper.readValue(response.firstRecord().getValue(0).asString(), this.entityClass))
                ));
    }

    public Multi<E> getByIds(List<String> ids, List<String> sortOrder) {
        String joinedString = StringUtils.wrap(StringUtils.join(ids, "', '"), "'");
        return Uni.createFrom().completionStage(request.query("select * from " + this.tableName + " where id in [" + joinedString + "]" + getSortOrder(sortOrder)).execute())
                .onItem()
                .transformToMulti(response -> Multi.createFrom().iterable(response.records())
                        .map(Unchecked.function(record -> mapper.toSecond(this.objectMapper.readValue(record.getValue(0).asString(), entityClass))))
                );
    }

    public Uni<E> patch(E e) {
        throw new NotImplementedException();
    }

    public Uni<E> put(E e) {
        throw new NotImplementedException();
    }

    public Multi<String> bulkCreate(List<E> eList) {
        throw new NotImplementedException();
    }

    public Uni<List<E>> bulkCreateWithResponse(List<E> eList) {
        throw new NotImplementedException();
    }

    public Uni<Void> bulkPatch(List<E> eList) {
        throw new NotImplementedException();
    }

    public Uni<BulkResponse> bulkUpsert(List<E> eList) {
        throw new NotImplementedException();
    }

    public Uni<E> delete(E e) {
        throw new NotImplementedException();
    }

    public Multi<E> findByPage(Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByCreatedAtGreaterThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByCreatedAtLessThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByCreatedAtBetween(Instant t1, Instant t2, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByLastModifiedAtGreaterThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByLastModifiedAtLessThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByLastModifiedAtBetween(Instant t1, Instant t2, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByLastDeletedAtGreaterThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByDeletedAtLessThan(Instant t1, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    public Multi<E> findByDeletedAtBetween(Instant t1, Instant t2, Integer offset, Integer limit, List<String> sortOrder) {
        throw new NotImplementedException();
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<E> findOne(@SpanAttribute("query.filter") String query) {
        return Uni.createFrom().completionStage(request.query(query).execute())
                .map(Unchecked.function(response -> ObjectUtils.isEmpty(response.records()) ? null : mapper.toSecond(
                        this.objectMapper.readValue(response.firstRecord().getValue(0).asString(), this.entityClass))
                ));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<E> findAll(@SpanAttribute("query.filter") String query) {
        return Uni.createFrom().completionStage(request.query(query).execute())
                .onItem()
                .transformToMulti(response -> Multi.createFrom().iterable(response.records())
                        .map(Unchecked.function(record -> mapper.toSecond(this.objectMapper.readValue(record.getValue(0).asString(), entityClass))))
                );
    }

    private String getSortOrder(List<String> sortOrder) {
        if (ObjectUtils.isEmpty(sortOrder)) return "";
        List<String> sort = new ArrayList<>();
        for (String s : sortOrder) {
            if (s.charAt(0) == '-') sort.add(s.substring(1) + "  DESC");
            else sort.add(s + "  ASC");
        }
        return "order by " + StringUtils.join(sort, ", ");
    }

}
