package com.services.sf.sql;

import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.base.AvroData;
import com.services.common.domain.basesql.BaseSqlDomain;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;

import java.util.List;

@IfBuildProperty(name = "framework.sql.enabled", stringValue = "true")
public interface BaseOutboxSqlService<T extends BaseSqlEntity, E extends BaseSqlDomain, C extends AbstractDomain>
        extends BaseSqlService<T, E, C> {

    Uni<E> create(E e, String aggregateType, String eventType);

    Uni<E> upsert(E e, E filter, String aggregateType, String eventType);

    Uni<E> patch(String id, E e, String aggregateType, String eventType);

    Uni<E> put(String id, E e, String aggregateType, String eventType);

    Uni<E> delete(E e, String aggregateType, String eventType);

    Uni<List<E>> bulkCreateWithResponse(List<E> es, String aggregateType, String eventType);

    AvroData getAvroData(E e);

    String getAggregateType(E domain);

    String getEventType(E domain);
}
