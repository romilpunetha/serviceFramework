package com.services.sf.commons.base;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.services.common.constant.GlobalConstant;
import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.base.BaseDomain;
import com.services.common.domain.base.BulkResponse;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.mutiny.redis.client.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseServiceImpl<T extends BaseEntity<ID>, E extends BaseDomain, C extends AbstractDomain, ID>
        implements BaseService<T, E, C, ID> {

    @ConfigProperty(name = "framework.redis.defaultExpiryInMilli", defaultValue = "3600000")
    protected Long defaultCacheExpiryInMilli;

    BaseRepository<T, E, ID> repository;
    BaseDomainCacheRepository<E, C> cache;

    public BaseServiceImpl(BaseRepository<T, E, ID> repository) {
        this.repository = repository;

        Infrastructure.setDroppedExceptionHandler(err -> Log.error(err.getMessage(), err));
    }

    public BaseServiceImpl(BaseRepository<T, E, ID> repository, BaseDomainCacheRepository<E, C> cache) {
        this(repository);
        this.cache = cache;
    }

    protected static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public Uni<E> get(String id) {
        return repository.get(id);
    }

    public Multi<E> getByIds(List<String> ids) {
        return this.getByIds(ids, new ArrayList<>());
    }

    public Multi<E> getByIds(List<String> ids, List<String> sortOrder) {
        return repository.getByIds(ids, sortOrder);
    }

    public Uni<E> create(E e) {
        return repository.create(e);
    }

    public Uni<E> upsert(E e, E filter) {
        return repository.upsert(e, filter);
    }

    public Uni<E> patch(String id, E e) {
        e.setId(id);
        return repository.patch(e);
    }

    public Uni<E> put(String id, E e) {
        e.setId(id);
        return repository.put(e);
    }

    public Uni<E> delete(E e) {
        return repository.delete(e);
    }

    public Multi<String> bulkCreate(List<E> eList) {
        return repository.bulkCreate(eList);
    }

    @Override
    public Uni<List<E>> bulkCreateWithResponse(List<E> es) {
        return repository.bulkCreateWithResponse(es);
    }

    public Uni<Void> bulkPatch(List<E> eList) {
        return repository.bulkPatch(eList);
    }

    public Uni<BulkResponse> bulkUpsert(List<E> eList) {
        return repository.bulkUpsert(eList);
    }

    public Multi<E> findByCreatedAtGreaterThan(Instant t1,
                                               Integer offset,
                                               Integer limit,
                                               List<String> sortOrder) {
        return repository.findByCreatedAtGreaterThan(t1, offset, limit, sortOrder);
    }

    public Multi<E> findByCreatedAtLessThan(Instant t1,
                                            Integer offset,
                                            Integer limit,
                                            List<String> sortOrder) {
        return repository.findByCreatedAtLessThan(t1, offset, limit, sortOrder);
    }

    public Multi<E> findByCreatedAtBetween(Instant t1,
                                           Instant t2,
                                           Integer offset,
                                           Integer limit,
                                           List<String> sortOrder) {
        return repository.findByCreatedAtBetween(t1, t2, offset, limit, sortOrder);
    }

    public Multi<E> findByLastModifiedAtGreaterThan(Instant t1,
                                                    Integer offset,
                                                    Integer limit,
                                                    List<String> sortOrder) {
        return repository.findByLastModifiedAtGreaterThan(t1, offset, limit, sortOrder);
    }

    public Multi<E> findByLastModifiedAtLessThan(Instant t1,
                                                 Integer offset,
                                                 Integer limit,
                                                 List<String> sortOrder) {
        return repository.findByLastModifiedAtLessThan(t1, offset, limit, sortOrder);
    }

    public Multi<E> findByLastModifiedBetween(Instant t1,
                                              Instant t2,
                                              Integer offset,
                                              Integer limit,
                                              List<String> sortOrder) {
        return repository.findByLastModifiedAtBetween(t1, t2, offset, limit, sortOrder)
                ;
    }

    protected Uni<E> getThroughCache(String id) {

        return getThroughCache(id, defaultCacheExpiryInMilli);
    }

    protected Uni<E> getThroughCache(String id, Long expiryInMilliseconds) {

        return getThroughCache(repository::get, id, expiryInMilliseconds);
    }

    protected Uni<E> getThroughCache(Function<String, Uni<E>> func, String param1, Long expiryInMilli) {

        Objects.requireNonNull(cache);

        return cache.get(param1)
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithNull()
                .onItem().ifNull().switchTo(
                        func.apply(param1)
                                .onItem().ifNotNull().call(Unchecked.function(e ->
                                        cache.set(param1, e, expiryInMilli)
                                                .onFailure().invoke(Log::error)
                                                .onFailure().recoverWithNull())
                                ));
    }

    protected Uni<E> getThroughCache(BiFunction<String, String, Uni<E>> func, String param1, String param2, Long expiryInMilli) {

        Objects.requireNonNull(cache);

        String key = param1 + GlobalConstant.DELIMITER + param2;

        return cache.get(key)
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithNull()
                .onItem().ifNull().switchTo(
                        func.apply(param1, param2)
                                .onItem().ifNotNull().call(Unchecked.function(e ->
                                        cache.set(key, e, expiryInMilli)
                                                .onFailure().invoke(Log::error)
                                                .onFailure().recoverWithNull())
                                ));
    }

    protected Uni<E> getThroughCacheHSet(BiFunction<String, String, Uni<E>> func, String hash, String key, Long expiryInMilli) {

        Objects.requireNonNull(cache);


        return cache.hget(hash, key)
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithNull()
                .onItem().ifNull().switchTo(
                        func.apply(hash, key)
                                .onItem().ifNotNull().call(Unchecked.function(e ->
                                        cache.hset(hash, key, e, expiryInMilli)
                                                .onFailure().invoke(Log::error)
                                                .onFailure().recoverWithNull())
                                ));
    }

    protected Uni<E> patchThroughCache(String id, E e) {

        Objects.requireNonNull(cache);

        return this.patch(id, e)
                .call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    protected Uni<E> putThroughCache(String id, E e) {

        Objects.requireNonNull(cache);

        return this.put(id, e)
                .call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    protected Uni<E> deleteThroughCache(E e) {

        Objects.requireNonNull(cache);

        return this.delete(e)
                .call(Unchecked.function(t -> invalidateCache(e.getId())))
                ;
    }

    public Multi<E> findByPage(Integer offset, Integer limit, List<String> sortOrder) {
        return repository.findByPage(offset, limit, sortOrder);
    }

    public Uni<E> hget(String hash, String field) {

        Objects.requireNonNull(cache);

        return cache.hget(hash, field);
    }

    public Uni<Response> hset(final String hash, String field, final E e) throws JsonProcessingException {

        Objects.requireNonNull(cache);


        return cache.hset(hash, field, e);
    }

    public Multi<String> hkeys(String hash) {

        Objects.requireNonNull(cache);

        return cache.hkeys(hash);
    }

    public Uni<Void> hdel(String hash, List<String> fields) {

        Objects.requireNonNull(cache);

        return cache.hdel(hash, fields);
    }

    public Multi<Optional<E>> hmget(String hash, List<String> keys) {

        Objects.requireNonNull(cache);

        return cache.hmget(hash, keys);
    }

    protected List<String> getUnCachedIds(List<String> ids, List<E> eCachedList) {

        if (ObjectUtils.isEmpty(ids))
            return Collections.emptyList();

        if (ObjectUtils.isEmpty(eCachedList))
            return ids;

        Set<String> cachedIds = eCachedList.stream()
                .filter(ObjectUtils::isNotEmpty)
                .map(E::getId)
                .collect(Collectors.toSet());

        return ids.stream()
                .filter(StringUtils::isNotBlank)
                .filter(id -> !cachedIds.contains(id))
                .collect(Collectors.toList());
    }

    protected Uni<List<E>> getByIdsThroughCache(List<String> ids) {

        return getByIdsThroughCache(ids, defaultCacheExpiryInMilli);
    }

    protected Uni<List<E>> getByIdsThroughCache(List<String> ids, Long expiryInMilliseconds) {

        Objects.requireNonNull(cache);


        return cache.get(ids).collect().asList()
                .map(optionals -> optionals.stream().map(optional -> optional.orElse(null)).filter(Objects::nonNull).collect(Collectors.toList()))
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithItem(Collections.emptyList())
                .chain(cachedItems -> {
                    List<String> unCachedIds = getUnCachedIds(ids, cachedItems);
                    return unCachedIds.isEmpty() ?
                            Uni.createFrom().item(cachedItems) :
                            repository.getByIds(unCachedIds, new ArrayList<>())
                                    .call(Unchecked.function(item -> cache.set(item.getId(), item, expiryInMilliseconds)
                                            .onFailure().invoke(Log::error)
                                            .onFailure().recoverWithNull()
                                    )).collect().asList()
                                    .map(items -> {
                                        items = ObjectUtils.isEmpty(items) ? new ArrayList<>() : items;
                                        items.addAll(cachedItems);
                                        return items;
                                    });
                });
    }

    protected Uni<Void> invalidateCache(String id) {

        Objects.requireNonNull(cache);

        return cache.delete(List.of(id))
                .onFailure().retry().atMost(3)
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithNull();
    }

    protected Uni<Void> invalidateCache(List<String> ids) {

        Objects.requireNonNull(cache);

        return cache.delete(ids)
                .onFailure().retry().atMost(3)
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithNull();
    }
}
