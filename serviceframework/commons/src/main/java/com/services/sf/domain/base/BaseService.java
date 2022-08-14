package com.services.sf.domain.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.base.BaseDomain;
import com.services.common.domain.basemongo.BulkResponse;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BaseService<T extends BaseEntity<ID>, E extends BaseDomain, C extends AbstractDomain, ID> {

    Uni<E> create(E e);

    Uni<E> upsert(E e, E filter);

    Uni<E> get(String id);

    Multi<E> getByIds(List<String> ids);

    Multi<E> getByIds(List<String> ids, List<String> sortOrder);

    Uni<E> patch(String id, E e);

    Uni<E> put(String id, E e);

    Uni<E> delete(E e);

    Multi<String> bulkCreate(List<E> eList);

    Uni<List<E>> bulkCreateWithResponse(List<E> eList);

    Uni<Void> bulkPatch(List<E> eList);

    Uni<BulkResponse> bulkUpsert(List<E> eList);

    Multi<E> findByCreatedAtGreaterThan(Instant t1,
                                        Integer offset,
                                        Integer limit,
                                        List<String> sortOrder);

    Multi<E> findByCreatedAtLessThan(Instant t1,
                                     Integer offset,
                                     Integer limit,
                                     List<String> sortOrder);

    Multi<E> findByCreatedAtBetween(Instant t1,
                                    Instant t2,
                                    Integer offset,
                                    Integer limit,
                                    List<String> sortOrder);

    Multi<E> findByLastModifiedAtGreaterThan(Instant t1,
                                             Integer offset,
                                             Integer limit,
                                             List<String> sortOrder);

    Multi<E> findByLastModifiedAtLessThan(Instant t1,
                                          Integer offset,
                                          Integer limit,
                                          List<String> sortOrder);

    Multi<E> findByLastModifiedBetween(Instant t1,
                                       Instant t2,
                                       Integer offset,
                                       Integer limit,
                                       List<String> sortOrder);

    Multi<E> findByPage(Integer offset, Integer limit, List<String> sortOrder);

    Uni<E> hget(String hash, String key);

    Uni<Response> hset(final String hash, String field, final E e) throws JsonProcessingException;

    Multi<String> hkeys(String hash);

    Uni<Void> hdel(String hash, List<String> fields);

    Multi<Optional<E>> hmget(String hash, List<String> keys);
}
