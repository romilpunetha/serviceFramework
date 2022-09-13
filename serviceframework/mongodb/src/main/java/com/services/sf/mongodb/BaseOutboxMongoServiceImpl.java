package com.services.sf.mongodb;

import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.common.enums.OutboxEvent;
import com.services.sf.mongodb.outbox.OutboxService;
import com.services.sf.mongodb.util.MongoHelper;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;


@NoArgsConstructor
@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
@IfBuildProperty(name = "framework.outbox.enabled", stringValue = "true")
public abstract class BaseOutboxMongoServiceImpl<T extends BaseMongoEntity, E extends BaseMongoDomain, C extends AbstractDomain>
        extends BaseMongoServiceImpl<T, E, C>
        implements BaseOutboxMongoService<T, E, C> {

    @Inject
    protected OutboxService outboxService;

    @Inject
    protected MongoHelper mongoHelper;

    public BaseOutboxMongoServiceImpl(BaseMongoRepository<T, E> repository) {
        this(repository, null);
    }

    public BaseOutboxMongoServiceImpl(BaseMongoRepository<T, E> repository, BaseMongoCacheRepository<E, C> cache) {
        super(repository, cache);
    }

    @Override
    public Uni<E> create(E e) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                            clientSession.startTransaction();
                            return this.create(e, clientSession)
                                    .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                                    .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                        }
                );

    }

    @Override
    public Uni<E> create(E e, ClientSession clientSession) {
        return super.create(e, clientSession)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), getAggregateType(createdE), getEventType(createdE), null),
                        clientSession
                )));
    }

    @Override
    public Uni<E> upsert(E e, E filter) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                            clientSession.startTransaction();
                            return this.upsert(e, filter, clientSession)
                                    .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                                    .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                        }
                );
    }

    @Override
    public Uni<E> upsert(E e, E filter, ClientSession clientSession) {
        return super.upsert(e, filter, clientSession)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), getAggregateType(createdE), getEventType(createdE), null),
                        clientSession
                )));
    }

    @Override
    public Uni<E> patch(String id, E e) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.patch(id, e, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    @Override
    public Uni<E> patch(String id, E e, ClientSession clientSession) {
        e.setId(id);
        return super.patch(id, e, clientSession)
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), getAggregateType(patchedE), getEventType(patchedE), null),
                        clientSession)
                ));
    }

    @Override
    public Uni<E> put(String id, E e) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.put(id, e, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    @Override
    public Uni<E> put(String id, E e, ClientSession clientSession) {
        e.setId(id);
        return super.put(id, e, clientSession)
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), getAggregateType(patchedE), getEventType(patchedE), null),
                        clientSession)
                ));
    }

    @Override
    public Uni<E> delete(E e) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.delete(e, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                })
                ;
    }

    @Override
    public Uni<E> delete(E e, ClientSession clientSession) {
        return super.delete(e, clientSession)
                .call(Unchecked.function(deletedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(deletedE), getAggregateType(deletedE), getEventType(deletedE), null),
                        clientSession)
                ));
    }

    @Override
    protected Uni<E> patchThroughCache(String id, E e) {

        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.patchThroughCache(id, e, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    @Override
    protected Uni<E> patchThroughCache(String id, E e, ClientSession clientSession) {
        return this.patch(id, e, clientSession)
                .call(Unchecked.function(t -> invalidateCache(t.getId())))
                ;
    }

    @Override
    protected Uni<E> putThroughCache(String id, E e) {

        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.putThroughCache(id, e, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    @Override
    protected Uni<E> putThroughCache(String id, E e, ClientSession clientSession) {

        return this.put(id, e, clientSession)
                .call(Unchecked.function(t -> invalidateCache(t.getId())))
                ;
    }

    @Override
    protected Uni<E> deleteThroughCache(E e) {

        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.deleteThroughCache(e, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    @Override
    protected Uni<E> deleteThroughCache(E e, ClientSession clientSession) {

        return this.delete(e, clientSession)
                .call(Unchecked.function(t -> invalidateCache(e.getId())))
                ;
    }

    public Uni<E> create(E e, String aggregateType, String eventType) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                            clientSession.startTransaction();
                            return this.create(e, aggregateType, eventType, clientSession)
                                    .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                                    .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                        }
                );

    }

    public Uni<E> create(E e, String aggregateType, String eventType, ClientSession clientSession) {
        return super.create(e, clientSession)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), aggregateType, eventType, null),
                        clientSession
                )));
    }

    public Uni<List<E>> bulkCreate(List<E> eList, String aggregateType, String eventType) {

        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.bulkCreate(eList, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    public Uni<List<E>> bulkCreate(List<E> eList, String aggregateType, String eventType, ClientSession clientSession) {
        return this.bulkCreate(eList, clientSession).collect().asList()
                .chain(ids -> this.getByIds(ids, null, clientSession).collect().asList())
                .call(Unchecked.function(createdEList -> outboxService.bulkCreate(
                        outboxService.convertToOutbox(createdEList.stream().map(this::getAvroData).collect(Collectors.toList()), aggregateType, eventType, null),
                        clientSession
                ).collect().asList()));
    }

    public Uni<E> upsert(E e, E filter, String aggregateType, String eventType) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                            clientSession.startTransaction();
                            return this.upsert(e, filter, aggregateType, eventType, clientSession)
                                    .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                                    .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                        }
                );

    }

    public Uni<E> upsert(E e, E filter, String aggregateType, String eventType, ClientSession clientSession) {
        return super.upsert(e, filter, clientSession)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), aggregateType, eventType, null),
                        clientSession)
                ));
    }

    public Uni<E> patch(String id, E e, String aggregateType, String eventType) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.patch(id, e, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    public Uni<E> patch(String id, E e, String aggregateType, String eventType, ClientSession clientSession) {
        e.setId(id);
        return super.patch(id, e, clientSession)
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), aggregateType, eventType, null),
                        clientSession)
                ));
    }

    public Uni<Void> bulkPatch(List<E> eList, String aggregateType, String eventType) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.bulkPatch(eList, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    public Uni<Void> bulkPatch(List<E> eList, String aggregateType, String eventType, ClientSession clientSession) {
        return this.bulkPatch(eList, clientSession)
                .call(Unchecked.function(patchedE -> outboxService.bulkCreate(
                        outboxService.convertToOutbox(eList.stream().map(this::getAvroData).collect(Collectors.toList()), aggregateType, eventType, null),
                        clientSession).collect().asList()
                ));
    }

    public Uni<E> put(String id, E e, String aggregateType, String eventType) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.put(id, e, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    public Uni<E> put(String id, E e, String aggregateType, String eventType, ClientSession clientSession) {
        e.setId(id);
        return super.put(id, e, clientSession)
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), aggregateType, eventType, null),
                        clientSession)
                ));
    }

    public Uni<E> delete(E e, String aggregateType, String eventType) {
        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.delete(e, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                })
                ;
    }

    public Uni<E> delete(E e, String aggregateType, String eventType, ClientSession clientSession) {
        return super.delete(e, clientSession)
                .call(Unchecked.function(deletedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(deletedE), aggregateType, eventType, null),
                        clientSession)
                ));
    }

    protected Uni<E> patchThroughCache(String id, E e, String aggregateType, String eventType) {

        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.patchThroughCache(id, e, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    protected Uni<E> patchThroughCache(String id, E e, String aggregateType, String eventType, ClientSession clientSession) {
        return this.patch(id, e, aggregateType, eventType, clientSession)
                .call(Unchecked.function(t -> invalidateCache(t.getId())))
                ;
    }

    protected Uni<E> putThroughCache(String id, String aggregateType, String eventType, E e) {

        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.putThroughCache(id, e, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    protected Uni<E> putThroughCache(String id, E e, String aggregateType, String eventType, ClientSession clientSession) {

        return this.put(id, e, aggregateType, eventType, clientSession)
                .call(Unchecked.function(t -> invalidateCache(t.getId())))
                ;
    }

    protected Uni<E> deleteThroughCache(E e, String aggregateType, String eventType) {

        return mongoHelper.createClientSessionOutbox()
                .chain(clientSession -> {
                    clientSession.startTransaction();
                    return this.deleteThroughCache(e, aggregateType, eventType, clientSession)
                            .call(() -> Uni.createFrom().publisher(clientSession.commitTransaction()))
                            .onFailure().call(() -> Uni.createFrom().publisher(clientSession.abortTransaction()));
                });
    }

    protected Uni<E> deleteThroughCache(E e, String aggregateType, String eventType, ClientSession clientSession) {

        return this.delete(e, aggregateType, eventType, clientSession)
                .call(Unchecked.function(t -> invalidateCache(e.getId())))
                ;
    }

    public String getAggregateType(E domain) {
        return domain.getClass().getSimpleName();
    }

    public String getEventType(E domain) {
        if (domain.getCreatedAt() == domain.getLastModifiedAt())
            return OutboxEvent.CREATE.name();

        else if (ObjectUtils.isNotEmpty(domain.getDeletedAt()))
            return OutboxEvent.DELETE.name();
        else
            return OutboxEvent.UPDATE.name();
    }

}
