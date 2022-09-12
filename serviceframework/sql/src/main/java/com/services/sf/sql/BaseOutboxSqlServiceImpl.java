package com.services.sf.sql;

import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.common.domain.basesql.Outbox;
import com.services.common.enums.OutboxEvent;
import com.services.sf.sql.outbox.OutboxService;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
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
public abstract class BaseOutboxSqlServiceImpl<T extends BaseSqlEntity, E extends BaseSqlDomain, C extends AbstractDomain>
        extends BaseSqlServiceImpl<T, E, C>
        implements BaseOutboxSqlService<T, E, C> {

    @Inject
    protected OutboxService outboxService;

    public BaseOutboxSqlServiceImpl(BaseSqlRepository<T, E> repository) {
        this(repository, null);
    }

    public BaseOutboxSqlServiceImpl(BaseSqlRepository<T, E> repository, BaseSqlCacheRepository<E, C> cache) {
        super(repository, cache);
    }

    @Override
    @ReactiveTransactional
    public Uni<E> create(E e) {
        return super.create(e)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), getAggregateType(createdE), getEventType(createdE), null)
                )));
    }

    @Override
    @ReactiveTransactional
    public Uni<E> upsert(E e, E filter) {
        return super.upsert(e, filter)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), getAggregateType(createdE), getEventType(createdE), null)
                )));
    }

    @Override
    @ReactiveTransactional
    public Uni<E> patch(String id, E e) {
        e.setId(id);
        return super.patch(id, e)
                .onItem().ifNotNull()
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), getAggregateType(patchedE), getEventType(patchedE), null)
                )));
    }

    @Override
    @ReactiveTransactional
    public Uni<E> put(String id, E e) {
        e.setId(id);
        return super.put(id, e)
                .onItem().ifNotNull()
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), getAggregateType(patchedE), getEventType(patchedE), null)
                )));
    }

    @Override
    @ReactiveTransactional
    public Uni<E> delete(E e) {
        return super.delete(e)
                .onItem().ifNotNull()
                .call(Unchecked.function(deletedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(deletedE), getAggregateType(deletedE), getEventType(deletedE), null)
                )));
    }

    @Override
    @ReactiveTransactional
    protected Uni<E> patchThroughCache(String id, E e) {
        return this.patch(id, e).call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    @Override
    @ReactiveTransactional
    protected Uni<E> putThroughCache(String id, E e) {
        return this.put(id, e).call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    @Override
    @ReactiveTransactional
    protected Uni<E> deleteThroughCache(E e) {
        return this.delete(e).call(Unchecked.function(t -> invalidateCache(e.getId())));
    }

    @Override
    @ReactiveTransactional
    public Uni<List<E>> bulkCreateWithResponse(List<E> es) {
        return super.bulkCreateWithResponse(es)
                .call(Unchecked.function(createdEs -> {
                    List<Outbox> createdOutboxes = createdEs.stream().map(Unchecked.function(e ->
                            outboxService.convertToOutbox(getAvroData(e), getAggregateType(e), getEventType(e), null))).collect(Collectors.toList());
                    return outboxService.bulkCreateWithResponse(createdOutboxes);
                }));
    }

    @ReactiveTransactional
    public Uni<E> create(E e, String aggregateType, String eventType) {
        return super.create(e)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), aggregateType, eventType, null)
                )));
    }

    @ReactiveTransactional
    public Uni<E> upsert(E e, E filter, String aggregateType, String eventType) {
        return super.upsert(e, filter)
                .call(Unchecked.function(createdE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(createdE), aggregateType, eventType, null)
                )));
    }

    @ReactiveTransactional
    public Uni<E> patch(String id, E e, String aggregateType, String eventType) {
        e.setId(id);
        return super.patch(id, e)
                .onItem().ifNotNull()
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), aggregateType, eventType, null)
                )));
    }

    @ReactiveTransactional
    public Uni<E> put(String id, E e, String aggregateType, String eventType) {
        e.setId(id);
        return super.put(id, e)
                .onItem().ifNotNull()
                .call(Unchecked.function(patchedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(patchedE), aggregateType, eventType, null)
                )));
    }

    @ReactiveTransactional
    public Uni<E> delete(E e, String aggregateType, String eventType) {
        return super.delete(e)
                .onItem().ifNotNull()
                .call(Unchecked.function(deletedE -> outboxService.create(
                        outboxService.convertToOutbox(getAvroData(deletedE), aggregateType, eventType, null)
                )));
    }

    @ReactiveTransactional
    protected Uni<E> patchThroughCache(String id, E e, String aggregateType, String eventType) {
        return this.patch(id, e, aggregateType, eventType).call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    @ReactiveTransactional
    protected Uni<E> putThroughCache(String id, E e, String aggregateType, String eventType) {
        return this.put(id, e, aggregateType, eventType).call(Unchecked.function(t -> invalidateCache(t.getId())));
    }

    @ReactiveTransactional
    protected Uni<E> deleteThroughCache(E e, String aggregateType, String eventType) {
        return this.delete(e, aggregateType, eventType).call(Unchecked.function(t -> invalidateCache(e.getId())));
    }

    @ReactiveTransactional
    public Uni<List<E>> bulkCreateWithResponse(List<E> es, String aggregateType, String eventType) {
        return super.bulkCreateWithResponse(es)
                .call(Unchecked.function(createdEs -> {
                    var createdOutboxes = createdEs.stream().map(Unchecked.function(e -> outboxService.convertToOutbox(getAvroData(e), aggregateType, eventType, null))).toList();
                    return outboxService.bulkCreateWithResponse(createdOutboxes);
                }));
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
