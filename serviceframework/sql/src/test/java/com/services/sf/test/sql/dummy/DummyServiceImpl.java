package com.services.sf.test.sql.dummy;

import com.services.common.domain.base.AvroData;
import com.services.sf.sql.BaseOutboxSqlServiceImpl;
import com.services.test.sql.avro.DummyEvent;
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

    public AvroData getAvroData(Dummy dummy) {
        DummyEvent dummyEvent = eventMapper.toSecond(dummy);
        return AvroData.builder()
                .payload(dummyEvent)
                .domain(dummy)
                .build();
    }
}