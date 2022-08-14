package com.services.sf.domain.base;

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
    protected ReactiveRedisClient asyncRedisClient;

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

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Boolean> exists(@SpanAttribute("query.id") final String id) {

        return asyncRedisClient.exists(List.of(getBucket(id)))
                .map(response -> Integer.parseInt(response.getDelegate().toString()) > 0);
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<C> get(@SpanAttribute("query.id") final String id) {
        return asyncRedisClient.get(getBucket(id))
                .onItem().ifNotNull().transform(Unchecked.function(response -> objectMapper.readValue(response.toString(), domainClass)));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> get(@SpanAttribute("query.id") final List<String> ids) {
        return asyncRedisClient.mget(ids.stream().map(this::getBucket).collect(Collectors.toList()))
                .onItem().transformToMulti(response -> Multi.createFrom().items(
                        response.getDelegate()
                                .stream()
                                .map(res -> {
                                    try {
                                        return ObjectUtils.isEmpty(res) ? Optional.empty() : Optional.of(objectMapper.readValue(res.toString(), domainClass));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                        return Optional.empty();
                                    }
                                })
                ));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<List<C>> getList(@SpanAttribute("query.id") String id) {
        return asyncRedisClient.get(getBucket(id))
                .onItem().ifNotNull().transform(Unchecked.function(response -> objectMapper.readValue(response.toString(), listType)));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final C t) throws JsonProcessingException {
        String value = objectMapper.writeValueAsString(t);
        return asyncRedisClient.set(Arrays.asList(getBucket(id), value));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final List<C> t) throws JsonProcessingException {
        String value = objectMapper.writeValueAsString(t);
        return asyncRedisClient.set(Arrays.asList(getBucket(id), value));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final List<C> t, Long expiryInMilliseconds) throws JsonProcessingException {
        String value = objectMapper.writeValueAsString(t);
        return asyncRedisClient.set(Arrays.asList(getBucket(id), value, "PX", expiryInMilliseconds.toString()));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> set(final String id, final C t, Long expiryInMilliseconds) throws JsonProcessingException {
        String value = objectMapper.writeValueAsString(t);
        return asyncRedisClient.set(Arrays.asList(getBucket(id), value, "PX", expiryInMilliseconds.toString()));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Void> delete(List<String> ids) {
        return asyncRedisClient.del(ids.stream().map(this::getBucket).collect(Collectors.toList()))
                .replaceWithVoid();
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Void> mset(final Map<String, C> mp) {

        List<String> keyValue = new ArrayList<>();
        mp.forEach((k, v) -> {
            keyValue.add(getBucket(k));
            try {
                keyValue.add(objectMapper.writeValueAsString(v));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                keyValue.add(null);
            }
        });

        return asyncRedisClient.mset(keyValue)
                .replaceWithVoid();
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> hset(@SpanAttribute("query.hash") final String hash, @SpanAttribute("query.key") String field, final C e) throws JsonProcessingException {
        return asyncRedisClient.hset(List.of(getBucket(hash), field, objectMapper.writeValueAsString(e)));
    }


    public Uni<Response> hset(String hash, String field, C e, Long timeInSeconds) throws JsonProcessingException {
        return this.hset(hash, field, e).call(() -> this.expire(hash, timeInSeconds));
    }

    public Uni<Response> hset(String hash, String field, C e, Instant instant) throws JsonProcessingException {
        return this.hset(hash, field, e).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> hmset(@SpanAttribute("query.hash") final String hash, final List<String> key, final List<C> values) {
        List<String> func = new ArrayList<>();
        func.add(getBucket(hash));

        if (key.size() != values.size()) {
            throw new BaseRuntimeException(
                    ErrorLevel.WARNING,
                    ErrorCode.BAD_REQUEST,
                    "Arrays are of different size",
                    "key and value array do not have the same length"
            );
        }

        IntStream.range(0, values.size()).boxed()
                .forEach(i -> {
                    func.add(key.get(i));
                    try {
                        func.add(objectMapper.writeValueAsString(values.get(i)));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        throw new BaseRuntimeException(
                                ErrorLevel.WARNING,
                                ErrorCode.BAD_REQUEST,
                                "JsonProcessingError",
                                e.getMessage()
                        );
                    }
                });

        return asyncRedisClient.hmset(func);
    }

    public Uni<Response> hmset(String hash, List<String> key, List<C> values, Long timeInSeconds) {
        return hmset(hash, key, values).call(() -> this.expire(hash, timeInSeconds));
    }

    public Uni<Response> hmset(String hash, List<String> key, List<C> values, Instant instant) {
        return hmset(hash, key, values).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<C> hget(@SpanAttribute("query.hash") final String hash, @SpanAttribute("query.key") final String id) {
        return asyncRedisClient.hget(getBucket(hash), id)
                .onItem().ifNotNull().transform(Unchecked.function(response -> objectMapper.readValue(response.toString(), domainClass)));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Map<String, C>> hgetall(@SpanAttribute("query.hash") final String hash) {
        return asyncRedisClient.hgetall(getBucket(hash))
                .map(Unchecked.function(response -> {
                    Map<String, C> mp = new HashMap<>();
                    for (String key : response.getKeys()) {
                        mp.put(key,
                                ObjectUtils.isEmpty(response.get(key)) ?
                                        null :
                                        objectMapper.readValue(response.get(key).toString(), domainClass));
                    }
                    return mp;
                }));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Void> hdel(@SpanAttribute("query.hash") final String hash, final List<String> fields) {

        List<String> args = new ArrayList<>();
        args.add(getBucket(hash));
        args.addAll(fields);

        return asyncRedisClient
                .hdel(args)
                .replaceWithVoid()
                ;
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<String> hkeys(@SpanAttribute("query.hash") final String hash) {
        return asyncRedisClient.hkeys(getBucket(hash))
                .onItem().transformToMulti(response -> response.toMulti().map(Response::toString));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> hmget(@SpanAttribute("query.hash") final String hash, final List<String> fields) {
        List<String> fieldList = new ArrayList<>(fields);
        fieldList.add(0, getBucket(hash));
        return asyncRedisClient.hmget(fieldList)
                .onItem().transformToMulti(response -> Multi.createFrom().items(
                        response.getDelegate()
                                .stream()
                                .map(res -> {
                                    try {
                                        return ObjectUtils.isEmpty(res) ? Optional.empty() : Optional.of(objectMapper.readValue(res.toString(), domainClass));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                        return Optional.empty();
                                    }
                                })
                ));
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

        List<String> func = new ArrayList<>();
        func.add(getBucket(hash));
        IntStream.range(0, values.size()).boxed()
                .forEach(i -> {
                    func.add(scores.get(i).toString());
                    try {
                        func.add(objectMapper.writeValueAsString(values.get(i)));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        throw new BaseRuntimeException(
                                ErrorLevel.WARNING,
                                ErrorCode.BAD_REQUEST,
                                "JsonProcessingError",
                                "Json could not be processed"
                        );
                    }
                });
        return asyncRedisClient.zadd(func);
    }

    public Uni<Response> zadd(String hash, List<C> values, List<Number> scores, Long timeInSeconds) {
        return this.zadd(hash, values, scores).call(() -> this.expire(hash, timeInSeconds));
    }

    public Uni<Response> zadd(String hash, List<C> values, List<Number> scores, Instant instant) {
        return this.zadd(hash, values, scores).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> zadd(@SpanAttribute("query.hash") String hash, Map<C, Number> map) {
        List<C> values = new ArrayList<>();
        List<Number> scores = new ArrayList<>();

        map.forEach((k, v) -> {
            values.add(k);
            scores.add(v);
        });
        return this.zadd(hash, values, scores);
    }

    public Uni<Response> zadd(String hash, Map<C, Number> map, Long timeInSeconds) {
        return this.zadd(hash, map).call(() -> this.expire(hash, timeInSeconds));
    }

    public Uni<Response> zadd(String hash, Map<C, Number> map, Instant instant) {
        return this.zadd(hash, map).call(() -> this.expireat(hash, instant));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Pair<C, Double>> zpopmin(@SpanAttribute("query.hash") String hash) {
        return asyncRedisClient.zpopmin(List.of(getBucket(hash)))
                .map(Unchecked.function(response -> {
                    if (ObjectUtils.isEmpty(response) || response.getKeys().isEmpty())
                        return null;
                    List<Object> responseList = response.getDelegate()
                            .stream()
                            .collect(Collectors.toList());
                    return new Pair<>(
                            objectMapper.readValue(responseList.get(0).toString(), domainClass),
                            Double.parseDouble(responseList.get(1).toString()));
                }));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Pair<C, Double>> zpopmax(@SpanAttribute("query.hash") String hash) {
        return asyncRedisClient.zpopmax(List.of(getBucket(hash)))
                .map(Unchecked.function(response -> {
                    if (ObjectUtils.isEmpty(response) || response.getKeys().isEmpty())
                        return null;
                    List<Object> responseList = response.getDelegate()
                            .stream()
                            .collect(Collectors.toList());
                    return new Pair<C, Double>(
                            objectMapper.readValue(responseList.get(0).toString(), domainClass),
                            Double.parseDouble(responseList.get(1).toString()));
                }));
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> zrange(@SpanAttribute("query.hash") String hash, @SpanAttribute("query.start") Integer start, @SpanAttribute("query.end") Integer end) {
        List<String> func = List.of(getBucket(hash), start.toString(), end.toString());
        return asyncRedisClient.zrange(func)
                .onItem().transformToMulti(response -> Multi.createFrom().iterable(response))
                .map(res -> {
                    try {
                        return Optional.of(objectMapper.readValue(res.toString(), domainClass));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                });
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Multi<Optional<C>> zrevrange(@SpanAttribute("query.hash") String hash, @SpanAttribute("query.start") Integer start, @SpanAttribute("query.end") Integer end) {
        List<String> func = List.of(getBucket(hash), start.toString(), end.toString());
        return asyncRedisClient.zrevrange(func)
                .onItem().transformToMulti(response -> Multi.createFrom().iterable(response))
                .map(res -> {
                    try {
                        return Optional.of(objectMapper.readValue(res.toString(), domainClass));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                });
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> expire(@SpanAttribute("query.hash") String key, Long timeInSeconds) {
        return asyncRedisClient.expire(getBucket(key), timeInSeconds.toString());
    }

    @WithSpan(kind = SpanKind.CLIENT)
    public Uni<Response> expireat(@SpanAttribute("query.hash") String key, Instant instant) {
        return asyncRedisClient.expireat(getBucket(key), String.valueOf(instant.toEpochMilli() / 1000));
    }

}

