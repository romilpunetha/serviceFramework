package com.services.sf.domain.basemongo;

import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.common.domain.basemongo.BulkResponse;
import com.services.common.exception.NotImplementedException;
import com.services.sf.domain.base.BaseServiceImpl;
import com.services.sf.domain.basemongo.util.MongoHelper;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bson.types.ObjectId;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Objects;


@NoArgsConstructor
@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public class BaseMongoServiceImpl<T extends BaseMongoEntity, E extends BaseMongoDomain, C extends AbstractDomain>
        extends BaseServiceImpl<T, E, C, ObjectId>
        implements BaseMongoService<T, E, C> {

    @Inject
    protected MongoHelper mongoHelper;
    BaseMongoRepository<T, E> repository;
    BaseMongoCacheRepository<E, C> cache;

    public BaseMongoServiceImpl(BaseMongoRepository<T, E> repository) {
        this(repository, null);
        this.repository = repository;
    }

    public BaseMongoServiceImpl(BaseMongoRepository<T, E> repository, BaseMongoCacheRepository<E, C> cache) {
        super(repository, cache);

        this.repository = repository;
        this.cache = cache;
    }

    public Uni<E> create(E e, ClientSession clientSession) {
        return repository.create(e, clientSession);
    }


    public Uni<E> getSecondaryPreferred(String id) {

        Uni<ClientSession> clientSession = mongoHelper.createClientSessionSecondary();

        return clientSession.chain(session -> {
            session.startTransaction();
            return this.get(id, session)
                    .eventually(session::commitTransaction);
        });
    }

    public Uni<E> get(String id) {
        return this.get(id, null);
    }

    public Uni<E> get(String id, ClientSession clientSession) {
        return repository.get(id, clientSession);
    }

    public Uni<E> upsert(E e, E filter, ClientSession clientSession) {
        return repository.upsert(e, filter, clientSession);
    }

    public Uni<E> patch(String id, E e) {
        return this.patch(id, e, null);
    }

    public Uni<E> patch(String id, E e, ClientSession clientSession) {
        e.setId(id);
        return repository.patch(e, clientSession);
    }

    public Uni<E> put(String id, E e) {
        return this.put(id, e, null);
    }

    @Override
    public Uni<List<E>> bulkCreateWithResponse(List<E> es) {
        return Uni.createFrom().failure(new NotImplementedException("bulkCreateWithResponse not implemented yet for mongo"));
    }

    public Uni<E> put(String id, E e, ClientSession clientSession) {
        e.setId(id);
        return repository.put(e, clientSession);
    }

    public Uni<E> delete(E e, ClientSession clientSession) {
        return repository.delete(e, clientSession);
    }

    public Multi<String> bulkCreate(List<E> eList, ClientSession clientSession) {
        return repository.bulkCreate(eList, clientSession);
    }

    public Uni<Void> bulkPatch(List<E> eList, ClientSession clientSession) {
        return repository.bulkPatch(eList, clientSession);
    }

    public Uni<BulkResponse> bulkUpsert(List<E> eList, ClientSession clientSession) {
        return repository.bulkUpsert(eList, clientSession);
    }

    public Multi<E> getByIdsSecondaryPreferred(List<String> ids, List<String> sortOrder) {

        Uni<ClientSession> clientSession = mongoHelper.createClientSessionSecondary();

        return clientSession.onItem().transformToMulti(session -> this.getByIds(ids, sortOrder, session));
    }


    public Multi<E> getByIds(List<String> ids, List<String> sortOrder, ClientSession clientSession) {
        return repository.getByIds(ids, sortOrder, clientSession)
                ;
    }

    protected Uni<E> getThroughCache(String id, ClientSession clientSession) {

        return this.getThroughCache(id, defaultCacheExpiryInMilli, clientSession);
    }

    protected Uni<E> getThroughCache(String id, Long expiryInMilliseconds, ClientSession clientSession) {

        Objects.requireNonNull(cache);

        return cache.get(id)
                .onFailure().invoke(Log::error)
                .onFailure().recoverWithNull()
                .onItem().ifNull().switchTo(
                        repository.get(id, clientSession)
                                .onItem().ifNotNull().call(Unchecked.function(item ->
                                        cache.set(id, item, expiryInMilliseconds)
                                                .onFailure().invoke(Log::error)
                                                .onFailure().recoverWithNull())
                                ));
    }

    protected Uni<E> patchThroughCache(String id, E e) {

        return this.patchThroughCache(id, e, null);
    }

    protected Uni<E> patchThroughCache(String id, E e, ClientSession clientSession) {

        Objects.requireNonNull(cache);


        e.setId(id);
        return repository.patch(e, clientSession)
                .call(Unchecked.function(t -> invalidateCache(t.getId())))
                ;
    }

    protected Uni<E> putThroughCache(String id, E e) {

        return this.putThroughCache(id, e, null);
    }

    protected Uni<E> putThroughCache(String id, E e, ClientSession clientSession) {

        Objects.requireNonNull(cache);

        e.setId(id);
        return repository.put(e, clientSession)
                .call(Unchecked.function(t -> invalidateCache(t.getId())))
                ;
    }

    protected Uni<E> deleteThroughCache(E e) {

        return this.deleteThroughCache(e, null);
    }

    protected Uni<E> deleteThroughCache(E e, ClientSession clientSession) {

        Objects.requireNonNull(cache);

        return this.delete(e, clientSession)
                .call(Unchecked.function(t -> invalidateCache(e.getId())));
    }


}
