package com.services.sf.domain.basemongo.outbox;

import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.base.AvroData;
import com.services.common.domain.basemongo.Outbox;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

public interface OutboxService {

    Uni<Outbox> create(Outbox e, ClientSession clientSession);

    Multi<String> bulkCreate(List<Outbox> eList, ClientSession clientSession);

    Outbox convertToOutbox(AvroData avroData, String aggregateType, String eventType, Map<String, Object> additionalFieldValues);

    List<Outbox> convertToOutbox(List<AvroData> avroData, String aggregateType, String eventType, Map<String, Object> additionalFieldValues);
}