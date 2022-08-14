package com.services.sf.domain.basemongo.outbox;

import com.services.common.domain.basemongo.Outbox;
import com.services.sf.domain.basemongo.BaseMongoRepository;

public interface OutboxRepository extends BaseMongoRepository<OutboxEntity, Outbox> {
}
