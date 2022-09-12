package com.services.sf.commons.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.services.common.domain.base.BaseDomain;
import com.services.common.domain.util.LocalContext;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jboss.marshalling.Pair;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor
@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.redis.enabled", stringValue = "true")
public abstract class BaseDomainCacheRepositoryImpl<E extends BaseDomain, C>
        implements BaseDomainCacheRepository<E, C> {

    protected Class<C> domainClass;
    protected String bucketPrefix;
    protected BaseCacheMapper<E, C> mapper;
    protected Boolean hasTenant;

    @Inject
    protected LocalContext localContext;

    @Inject
    protected BaseCacheRepositoryImpl<C> baseCacheRepository;

    public BaseDomainCacheRepositoryImpl(@NotNull BaseCacheMapper<E, C> mapper,
                                         @NotNull Class<C> domainClass,
                                         @NotNull String bucketPrefix) {
        this.mapper = mapper;
        this.domainClass = domainClass;
        this.bucketPrefix = bucketPrefix;
        this.hasTenant = true;
    }

    public BaseDomainCacheRepositoryImpl(@NotNull BaseCacheMapper<E, C> mapper,
                                         @NotNull Class<C> domainClass,
                                         @NotNull String bucketPrefix,
                                         @NotNull Boolean hasTenant) {
        this(mapper, domainClass, bucketPrefix);
        this.hasTenant = hasTenant;
    }

    @PostConstruct
    public void setBaseCacheRepository() {
        this.baseCacheRepository.setDomainClass(this.domainClass);
        this.baseCacheRepository.setBucketPrefix(this.bucketPrefix);
        this.baseCacheRepository.setHasTenant(this.hasTenant);
    }


    public Uni<Boolean> exists(final String id) {
        return baseCacheRepository.exists(id);
    }

    public Uni<E> get(final String id) {
        return baseCacheRepository.get(id).map(mapper::toFirst);
    }

    public Multi<Optional<E>> get(List<String> ids) {
        return baseCacheRepository.get(ids).map(optional -> optional.map(mapper::toFirst));
    }

    public Uni<List<E>> getList(String id) {
        return baseCacheRepository.getList(id).map(mapper::toFirst);
    }

    public Uni<Response> set(String id, E t) throws JsonProcessingException {
        return baseCacheRepository.set(id, mapper.toSecond(t));
    }

    public Uni<Response> set(String id, E t, Long expiryInMilliseconds) throws JsonProcessingException {
        return baseCacheRepository.set(id, mapper.toSecond(t), expiryInMilliseconds);
    }

    public Uni<Response> set(String id, List<E> t) throws JsonProcessingException {
        return baseCacheRepository.set(id, mapper.toSecond(t));
    }

    public Uni<Response> set(String id, List<E> t, Long expiryInMilliseconds) throws JsonProcessingException {
        return baseCacheRepository.set(id, mapper.toSecond(t), expiryInMilliseconds);
    }

    public Uni<Void> delete(List<String> ids) {
        return baseCacheRepository.delete(ids);
    }


    public Uni<Void> mset(Map<String, E> mp) {
        return baseCacheRepository.mset(mp.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, value -> mapper.toSecond(value.getValue()))));
    }


    public Uni<Response> hset(String hash, String field, E e) throws JsonProcessingException {
        return baseCacheRepository.hset(hash, field, mapper.toSecond(e));
    }


    public Uni<Response> hset(String hash, String field, E e, Long timeInSeconds) throws JsonProcessingException {
        return baseCacheRepository.hset(hash, field, mapper.toSecond(e), timeInSeconds);
    }


    public Uni<Response> hset(String hash, String field, E e, Instant instant) throws JsonProcessingException {
        return baseCacheRepository.hset(hash, field, mapper.toSecond(e), instant);
    }


    public Uni<Response> hmset(String hash, List<String> key, List<E> values) {
        return baseCacheRepository.hmset(hash, key, mapper.toSecond(values));
    }


    public Uni<Response> hmset(String hash, List<String> key, List<E> values, Long timeInSeconds) {
        return baseCacheRepository.hmset(hash, key, mapper.toSecond(values), timeInSeconds);
    }


    public Uni<Response> hmset(String hash, List<String> key, List<E> values, Instant instant) {
        return baseCacheRepository.hmset(hash, key, mapper.toSecond(values), instant);
    }


    public Uni<E> hget(String hash, String field) {
        return baseCacheRepository.hget(hash, field).map(mapper::toFirst);
    }


    public Uni<Map<String, E>> hgetall(String hash) {
        return baseCacheRepository.hgetall(hash)
                .map(mp -> mp.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, value -> mapper.toFirst(value.getValue()))));
    }


    public Uni<Void> hdel(String hash, List<String> fields) {
        return baseCacheRepository.hdel(hash, fields);
    }


    public Multi<String> hkeys(String hash) {
        return baseCacheRepository.hkeys(hash);
    }


    public Multi<Optional<E>> hmget(String hash, List<String> fields) {
        return baseCacheRepository.hmget(hash, fields).map(optional -> optional.map(mapper::toFirst));
    }


    public Uni<Response> zadd(String hash, Map<E, Number> map) {
        return baseCacheRepository.zadd(hash, map.entrySet().stream()
                .collect(Collectors.toMap(item -> mapper.toSecond(item.getKey()), Map.Entry::getValue)));
    }


    public Uni<Response> zadd(String hash, Map<E, Number> map, Long timeInSeconds) {
        return baseCacheRepository.zadd(hash, map.entrySet().stream()
                .collect(Collectors.toMap(item -> mapper.toSecond(item.getKey()), Map.Entry::getValue)), timeInSeconds);
    }


    public Uni<Response> zadd(String hash, Map<E, Number> map, Instant instant) {
        return baseCacheRepository.zadd(hash, map.entrySet().stream()
                .collect(Collectors.toMap(item -> mapper.toSecond(item.getKey()), Map.Entry::getValue)), instant);
    }


    public Uni<Response> zadd(String hash, List<E> values, List<Number> scores) {
        return baseCacheRepository.zadd(hash, values.stream().map(mapper::toSecond).collect(Collectors.toList()), scores);

    }


    public Uni<Response> zadd(String hash, List<E> values, List<Number> scores, Long timeInSeconds) {
        return baseCacheRepository.zadd(hash, values.stream().map(mapper::toSecond).collect(Collectors.toList()), scores, timeInSeconds);
    }


    public Uni<Response> zadd(String hash, List<E> values, List<Number> scores, Instant instant) {
        return baseCacheRepository.zadd(hash, values.stream().map(mapper::toSecond).collect(Collectors.toList()), scores, instant);
    }


    public Uni<Pair<E, Double>> zpopmax(String hash) {
        return baseCacheRepository.zpopmax(hash).map(cDoublePair -> new Pair<>(mapper.toFirst(cDoublePair.getA()), cDoublePair.getB()));
    }


    public Uni<Pair<E, Double>> zpopmin(String hash) {
        return baseCacheRepository.zpopmin(hash).map(cDoublePair -> new Pair<>(mapper.toFirst(cDoublePair.getA()), cDoublePair.getB()));
    }


    public Multi<Optional<E>> zrange(String hash, Integer start, Integer end) {
        return baseCacheRepository.zrange(hash, start, end).map(optional -> optional.map(mapper::toFirst));
    }


    public Multi<Optional<E>> zrevrange(String hash, Integer start, Integer end) {
        return baseCacheRepository.zrevrange(hash, start, end).map(optional -> optional.map(mapper::toFirst));
    }


    public Uni<Response> expire(String key, Long timeInSeconds) {
        return baseCacheRepository.expire(key, timeInSeconds);
    }


    public Uni<Response> expireat(String key, Instant instant) {
        return baseCacheRepository.expireat(key, instant);
    }

}

