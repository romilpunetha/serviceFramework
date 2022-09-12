package com.services.sf.mongodb.outbox;

import com.services.common.domain.basemongo.Outbox;
import com.services.sf.mongodb.BaseMongoRepository;

public interface OutboxRepository extends BaseMongoRepository<OutboxEntity, Outbox> {
}
