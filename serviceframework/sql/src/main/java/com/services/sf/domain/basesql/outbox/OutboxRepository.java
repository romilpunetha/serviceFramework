package com.services.sf.domain.basesql.outbox;

import com.services.common.domain.basesql.Outbox;
import com.services.sf.domain.basesql.BaseSqlRepository;
import io.smallrye.mutiny.Multi;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public interface OutboxRepository extends BaseSqlRepository<OutboxEntity, Outbox> {
    Multi<Outbox> findByAggregateTypeAndCreatedAtBetween(@NotBlank String aggregateType,
                                                         @NotNull Instant t1,
                                                         @NotNull Instant t2,
                                                         @Min(0) Integer offset,
                                                         @Min(1) Integer limit,
                                                         List<String> sortOrder);
}
