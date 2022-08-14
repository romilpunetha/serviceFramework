package com.services.sf.test.domain.mongo;

import com.services.serviceFramework.mongodb.avro.DummyEvent;
import com.services.sf.domain.base.BaseAvroMapper;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "cdi",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        imports = ObjectId.class)
public interface DummyEventMapper extends BaseAvroMapper<Dummy, DummyEvent> {
}
