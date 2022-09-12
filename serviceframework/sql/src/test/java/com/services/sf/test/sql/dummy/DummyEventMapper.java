package com.services.sf.test.sql.dummy;

import com.services.sf.commons.base.BaseAvroMapper;
import com.services.test.sql.avro.DummyEvent;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "cdi",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DummyEventMapper extends BaseAvroMapper<Dummy, DummyEvent> {
}
