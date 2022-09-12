package com.services.sf.sql.outbox;

import com.querydsl.core.types.dsl.Expressions;
import com.services.common.domain.basesql.Outbox;
import com.services.sf.sql.BaseSqlRepositoryImpl;
import io.smallrye.mutiny.Multi;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class OutboxRepositoryImpl
        extends BaseSqlRepositoryImpl<OutboxEntity, Outbox, QOutboxEntity>
        implements OutboxRepository {

    @Inject
    public OutboxRepositoryImpl(OutboxMapper mapper) {
        super(mapper, OutboxEntity.class, QOutboxEntity.class, false);
    }

    @Override
    public Multi<Outbox> findByAggregateTypeAndCreatedAtBetween(String aggregateType, Instant t1, Instant t2, Integer offset, Integer limit, List<String> sortOrder) {
        HQLQuery<OutboxEntity> query = newQuery()
                .where(currentEntity().aggregateType.eq(aggregateType).and(currentEntity().createdAt.between(Expressions.constant(t1), Expressions.constant(t2)))
                )
                .orderBy(sortOrder)
                .build();
        return this.findMany(query, Limits.of(offset, limit)).map(mapper::toSecond);
    }

}
