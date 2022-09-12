package com.services.sf.mongodb;

import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.base.AvroData;
import com.services.common.domain.basemongo.BaseMongoDomain;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;

import java.util.List;

@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public interface BaseOutboxMongoService<T extends BaseMongoEntity, E extends BaseMongoDomain, C extends AbstractDomain>
        extends BaseMongoService<T, E, C> {

    Uni<E> create(E e, String aggregateType, String eventType);

    Uni<E> create(E e, String aggregateType, String eventType, ClientSession clientSession);

    Uni<E> upsert(E e, E filter, String aggregateType, String eventType);

    Uni<E> upsert(E e, E filter, String aggregateType, String eventType, ClientSession clientSession);

    Uni<E> patch(String id, E e, String aggregateType, String eventType);

    Uni<E> patch(String id, E e, String aggregateType, String eventType, ClientSession clientSession);

    Uni<E> put(String id, E e, String aggregateType, String eventType);

    Uni<E> delete(E e, String aggregateType, String eventType);

    Uni<E> delete(E e, String aggregateType, String eventType, ClientSession clientSession);

    Uni<List<E>> bulkCreate(List<E> eList, String aggregateType, String eventType);

    Uni<List<E>> bulkCreate(List<E> eList, String aggregateType, String eventType, ClientSession clientSession);

    Uni<Void> bulkPatch(List<E> eList, String aggregateType, String eventType);

    Uni<Void> bulkPatch(List<E> eList, String aggregateType, String eventType, ClientSession clientSession);

    AvroData getAvroData(E e);

    String getAggregateType(E domain);

    String getEventType(E domain);
}
