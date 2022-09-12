package com.services.sf.test.mongo;

import com.services.common.domain.base.AvroData;
import com.services.sf.mongo.test.avro.DummyEvent;
import com.services.sf.mongodb.BaseOutboxMongoServiceImpl;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class DummyServiceImpl
        extends BaseOutboxMongoServiceImpl<DummyEntity, Dummy, DummyCache>
        implements DummyService {

    @Inject
    DummyEventMapper eventMapper;

    @Inject
    protected DummyServiceImpl(DummyRepository repository) {
        super(repository);
    }

    @Override
    public AvroData getAvroData(Dummy dummy) {
        DummyEvent dummyEvent = eventMapper.toSecond(dummy);
        return AvroData.builder()
                .payload(dummyEvent)
                .domain(dummy)
                .build();
    }
}
