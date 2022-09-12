package com.services.sf.sql.outbox;

import com.services.common.domain.base.AvroData;
import com.services.common.domain.basesql.Outbox;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

public interface OutboxService {

    Uni<Outbox> create(Outbox e);

    Multi<String> bulkCreate(List<Outbox> eList);

    Uni<List<Outbox>> bulkCreateWithResponse(List<Outbox> eList);

    Outbox convertToOutbox(AvroData avroData, String aggregateType, String eventType, Map<String, Object> additionalFieldValues);
}