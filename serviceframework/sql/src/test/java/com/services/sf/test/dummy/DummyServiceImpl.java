package com.services.sf.test.dummy;

import com.services.common.domain.base.AvroData;
import com.services.serviceFramework.sql.avro.DummyEvent;
import com.services.sf.domain.basesql.BaseOutboxSqlServiceImpl;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class DummyServiceImpl
        extends BaseOutboxSqlServiceImpl<DummyEntity, Dummy, DummyCache>
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