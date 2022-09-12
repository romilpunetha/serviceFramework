package com.services.sf.sql;

import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.base.BulkResponse;
import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.sf.commons.base.BaseServiceImpl;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;


@NoArgsConstructor
@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseSqlServiceImpl<T extends BaseSqlEntity, E extends BaseSqlDomain, C extends AbstractDomain>
        extends BaseServiceImpl<T, E, C, Long>
        implements BaseSqlService<T, E, C> {

    BaseSqlRepository<T, E> repository;
    BaseSqlCacheRepository<E, C> cache;

    public BaseSqlServiceImpl(BaseSqlRepository<T, E> repository) {
        this(repository, null);
        this.repository = repository;
    }

    public BaseSqlServiceImpl(BaseSqlRepository<T, E> repository, BaseSqlCacheRepository<E, C> cache) {
        super(repository, cache);
        this.repository = repository;
        this.cache = cache;
    }

    @Override
    public Uni<E> get(String id) {
        return repository.get(id);
    }

    @Override
    public Uni<E> create(E e) {
        return repository.create(e);
    }

    @Override
    public Uni<E> patch(String id, E e) {
        e.setId(id);
        return repository.patch(e);
    }

    @Override
    public Uni<E> upsert(E e, E filter) {
        return repository.upsert(e, filter);
    }

    @Override
    public Uni<E> put(String id, E e) {
        e.setId(id);
        return repository.put(e);
    }

    @Override
    public Uni<E> delete(E e) {
        return repository.delete(e);
    }

    @Override
    public Multi<String> bulkCreate(List<E> es) {
        return repository.bulkCreate(es);
    }

    @Override
    public Uni<List<E>> bulkCreateWithResponse(List<E> es) {
        return repository.bulkCreateWithResponse(es);
    }

    @Override
    public Uni<Void> bulkPatch(List<E> es) {
        return repository.bulkPatch(es);
    }

    @Override
    public Uni<BulkResponse> bulkUpsert(List<E> es) {
        return repository.bulkUpsert(es);
    }

    protected Uni<E> getThroughCache(String id) {

        return this.getThroughCache(id, defaultCacheExpiryInMilli);
    }

    protected Uni<E> getThroughCache(String id, Long expiryInMilliseconds) {

        Objects.requireNonNull(cache);

        return cache.get(id)
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithNull()
                .onItem().ifNull().switchTo(
                        repository.get(id)
                                .onItem().ifNotNull().call(Unchecked.function(item ->
                                        cache.set(id, item, expiryInMilliseconds)
                                                .onFailure().invoke(Log::error)
                                                .onFailure().recoverWithNull())
                                ));
    }

    protected Uni<E> patchThroughCache(String id, E e) {

        Objects.requireNonNull(cache);

        e.setId(id);
        return repository.patch(e)
                .call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    protected Uni<E> putThroughCache(String id, E e) {

        Objects.requireNonNull(cache);

        e.setId(id);
        return repository.put(e)
                .call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    protected Uni<E> deleteThroughCache(E e) {

        Objects.requireNonNull(cache);

        return this.delete(e)
                .call(Unchecked.function(t -> invalidateCache(e.getId())));
    }
}
