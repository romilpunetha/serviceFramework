package com.services.sf.commons.base;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.services.common.constant.GlobalConstant;
import com.services.common.domain.util.LocalContext;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;
import com.services.common.exception.BaseRuntimeException;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.redis.client.Response;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.jboss.marshalling.Pair;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Dependent
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.redis.enabled", stringValue = "true")
public class BaseCacheRepositoryImpl<C>
        implements BaseCacheRepository<C> {

   final ObjectMapper objectMapper;
    @Inject
    protected LocalContext localContext;

    @Inject
    protected ReactiveRedisDataSource reactiveRedisDataSource;

    @Setter
    Boolean hasTenant;

    JavaType listType;

    Class<C> domainClass;

    @Setter
    String bucketPrefix;

    public BaseCacheRepositoryImpl() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public BaseCacheRepositoryImpl(@NotNull Class<C> domainClass,
                                   @NotNull String bucketPrefix) {
        this();
        this.domainClass = domainClass;
        this.bucketPrefix = bucketPrefix;
        this.listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, domainClass);
        this.hasTenant = true;
    }

    public BaseCacheRepositoryImpl(@NotNull Class<C> domainClass,
                                   @NotNull String bucketPrefix,
                                   @NotNull Boolean hasTenant) {
        this(domainClass, bucketPrefix);
        this.hasTenant = hasTenant;
    }

    public void setDomainClass(Class<C> clazz) {
        this.domainClass = clazz;
        this.listType = new ObjectMapper().getTypeFactory().constructCollectionType(List.class, domainClass);
    }

    private String getBucket(String id) {
        Log.debug("LocalContext in Cache : " + localContext);
        return (this.hasTenant ? localContext.getTenantId() : "") +
                GlobalConstant.DELIMITER +
                this.bucketPrefix +
                GlobalConstant.DELIMITER +
                id;
    }

    private Response okResponse() {
        return Response.newInstance(SimpleStringType.OK);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Boolean> exists(@SpanAttribute("query.id") final String id) {
        return reactiveRedisDataSource.key(String.class).exists(getBucket(id));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<C> get(@SpanAttribute("query.id") final String id) {
        return reactiveRedisDataSource.string(domainClass).get(getBucket(id));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> get(@SpanAttribute("query.id") final List<String> ids) {
        String[] keys = ids.stream().map(this::getBucket).toArray(String[]::new);
        return reactiveRedisDataSource.string(domainClass).mget(keys)
                .onItem().transformToMulti(keyValueMap -> Multi.createFrom().emitter(multiEmitter -> {
                            Arrays.stream(keys).forEach(key -> multiEmitter.emit(keyValueMap.containsKey(key) ? Optional.of(keyValueMap.get(key)) : Optional.empty()));
                            multiEmitter.complete();
                        })
                );
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<List<C>> getList(@SpanAttribute("query.id") String id) {
        return reactiveRedisDataSource.string(String.class).get(getBucket(id))
                .onItem().ifNotNull().transform(Unchecked.function(a -> objectMapper.readValue(a, listType)));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final C t) throws JsonProcessingException {
        return reactiveRedisDataSource.string(domainClass).set(getBucket(id), t).replaceWith(this::okResponse);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final List<C> t) throws JsonProcessingException {
        return reactiveRedisDataSource.string(String.class).set(getBucket(id), objectMapper.writeValueAsString(t)).replaceWith(this::okResponse);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final List<C> t, Long expiryInMilliseconds) throws JsonProcessingException {
        SetArgs setArgs = new SetArgs();
        setArgs.px(expiryInMilliseconds);
        return reactiveRedisDataSource.string(String.class).set(getBucket(id), objectMapper.writeValueAsString(t), setArgs)
                .replaceWith(this::okResponse);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final C t, Long expiryInMilliseconds) throws JsonProcessingException {
        SetArgs setArgs = new SetArgs();
        setArgs.px(expiryInMilliseconds);
        return reactiveRedisDataSource.string(domainClass).set(getBucket(id), t, setArgs)
                .replaceWith(this::okResponse);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Void> delete(List<String> ids) {
        return reactiveRedisDataSource.key(String.class).del(ids.stream().map(this::getBucket).toArray(String[]::new))
                .replaceWithVoid();
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Void> mset(final Map<String, C> mp) {
        return reactiveRedisDataSource.string(domainClass).mset(mp.entrySet().stream().collect(Collectors.toMap(keyValue -> getBucket(keyValue.getKey()), Map.Entry::getValue)));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> hset(@SpanAttribute("query.hash") final String hash, @SpanAttribute("query.key") String field, final C e) throws JsonProcessingException {
        return reactiveRedisDataSource.hash(domainClass).hset(getBucket(hash), field, e)
                .map(wasSet -> wasSet ? 1 : 0).map(returnValue -> Response.newInstance(NumberType.create(returnValue)));
    }


    public Uni<Response> hset(String hash, String field, C e, Long timeInSeconds) throws JsonProcessingException {
        return this.hset(hash, field, e).call(() -> this.expire(hash, timeInSeconds));
    }

    public Uni<Response> hset(String hash, String field, C e, Instant instant) throws JsonProcessingException {
        return this.hset(hash, field, e).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    @Deprecated(since = "3.12.0")
    public Uni<Response> hmset(@SpanAttribute("query.hash") final String hash, final List<String> key, final List<C> values) {
        if (key.size() != values.size()) {
            throw new BaseRuntimeException(
                    ErrorLevel.WARNING,
                    ErrorCode.BAD_REQUEST,
                    "Arrays are of different size",
                    "key and value array do not have the same length"
            );
        }

        // TODO replace with hset?
        return this.reactiveRedisDataSource.hash(domainClass).hmset(getBucket(hash), IntStream.range(0, key.size()).boxed().collect(Collectors.toMap(key::get, values::get))
        ).replaceWith(this::okResponse);
    }

    @Deprecated(since = "3.12.0")
    public Uni<Response> hmset(String hash, List<String> key, List<C> values, Long timeInSeconds) {
        return hmset(hash, key, values).call(() -> this.expire(hash, timeInSeconds));
    }

    @Deprecated(since = "3.12.0")
    public Uni<Response> hmset(String hash, List<String> key, List<C> values, Instant instant) {
        return hmset(hash, key, values).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<C> hget(@SpanAttribute("query.hash") final String hash, @SpanAttribute("query.key") final String id) {
        return reactiveRedisDataSource.hash(domainClass).hget(getBucket(hash), id);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Map<String, C>> hgetall(@SpanAttribute("query.hash") final String hash) {
        return reactiveRedisDataSource.hash(domainClass).hgetall(getBucket(hash));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Void> hdel(@SpanAttribute("query.hash") final String hash, final List<String> fields) {
        return reactiveRedisDataSource.hash(domainClass).hdel(getBucket(hash), fields.toArray(String[]::new)).replaceWithVoid();
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<String> hkeys(@SpanAttribute("query.hash") final String hash) {
        return reactiveRedisDataSource.hash(domainClass).hkeys(getBucket(hash))
                .onItem().transformToMulti(keys -> Multi.createFrom().iterable(keys));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> hmget(@SpanAttribute("query.hash") final String hash, final List<String> fields) {
        return reactiveRedisDataSource.hash(domainClass).hmget(getBucket(hash), fields.toArray(String[]::new))
                .onItem().transformToMulti(keyValueMap -> Multi.createFrom().emitter(multiEmitter -> {
                            fields.forEach(field -> multiEmitter.emit(keyValueMap.containsKey(field) ? Optional.of(keyValueMap.get(field)) : Optional.empty()));
                            multiEmitter.complete();
                        })
                );
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> zadd(@SpanAttribute("query.hash") String hash, List<C> values, List<Number> scores) {

        if (values.size() != scores.size()) {
            throw new BaseRuntimeException(
                    ErrorLevel.WARNING,
                    ErrorCode.BAD_REQUEST,
                    "Arrays are of different size",
                    "values and scores array do not have the same length"
            );
        }

        return zadd(hash, IntStream.range(0, values.size()).boxed().collect(Collectors.toMap(values::get, scores::get)));

    }

    public Uni<Response> zadd(String hash, List<C> values, List<Number> scores, Long timeInSeconds) {
        return this.zadd(hash, values, scores).call(() -> this.expire(hash, timeInSeconds));
    }

    public Uni<Response> zadd(String hash, List<C> values, List<Number> scores, Instant instant) {
        return this.zadd(hash, values, scores).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> zadd(@SpanAttribute("query.hash") String hash, Map<C, Number> map) {
        return reactiveRedisDataSource.sortedSet(domainClass)
                .zadd(getBucket(hash), map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().doubleValue())))
                .map(count -> Response.newInstance(NumberType.create(count)));
    }

    public Uni<Response> zadd(String hash, Map<C, Number> map, Long timeInSeconds) {
        return this.zadd(hash, map).call(() -> this.expire(hash, timeInSeconds));
    }

    public Uni<Response> zadd(String hash, Map<C, Number> map, Instant instant) {
        return this.zadd(hash, map).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Pair<C, Double>> zpopmin(@SpanAttribute("query.hash") String hash) {
        return reactiveRedisDataSource.sortedSet(domainClass).zpopmin(getBucket(hash)).map(pair -> Pair.create(pair.value(), pair.score()));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Pair<C, Double>> zpopmax(@SpanAttribute("query.hash") String hash) {
        return reactiveRedisDataSource.sortedSet(domainClass).zpopmax(getBucket(hash)).map(pair -> Pair.create(pair.value(), pair.score()));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> zrange(@SpanAttribute("query.hash") String hash, @SpanAttribute("query.start") Integer start, @SpanAttribute("query.end") Integer end) {
        return reactiveRedisDataSource.sortedSet(domainClass).zrange(getBucket(hash), start, end)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list.stream().map(Optional::of).collect(Collectors.toList())));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> zrevrange(@SpanAttribute("query.hash") String hash, @SpanAttribute("query.start") Integer start, @SpanAttribute("query.end") Integer end) {
        ZRangeArgs zRangeArgs = new ZRangeArgs();
        zRangeArgs.rev();
        return reactiveRedisDataSource.sortedSet(domainClass).zrange(getBucket(hash), start, end, zRangeArgs)
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list.stream().map(Optional::of).collect(Collectors.toList())));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> expire(@SpanAttribute("query.hash") String key, Long timeInSeconds) {
        return reactiveRedisDataSource.key().expire(getBucket(key), timeInSeconds)
                // Same as redis
                .map(wasSet -> wasSet ? Response.newInstance(NumberType.create(1)) : Response.newInstance(NumberType.create(0)));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> expireat(@SpanAttribute("query.hash") String key, Instant instant) {
        return reactiveRedisDataSource.key().expireat(getBucket(key), instant)
                // Same as redis
                .map(wasSet -> wasSet ? Response.newInstance(NumberType.create(1)) : Response.newInstance(NumberType.create(0)));
    }
}


