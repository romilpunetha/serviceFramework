package com.services.sf.test.dummy;

import com.services.serviceFramework.sql.avro.DummyEvent;
import com.services.sf.domain.base.BaseAvroMapper;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "cdi",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DummyEventMapper extends BaseAvroMapper<Dummy, DummyEvent> {
}
