package com.services.sf.domain.basemongo.outbox;

import com.services.common.domain.basemongo.Outbox;
import com.services.sf.domain.basemongo.BaseMongoRepositoryImpl;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class OutboxRepositoryImpl
        extends BaseMongoRepositoryImpl<OutboxEntity, Outbox>
        implements OutboxRepository {

    @Inject
    public OutboxRepositoryImpl(OutboxMapper mapper) {
        super(mapper, false);
    }

}
