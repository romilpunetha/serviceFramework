package com.services.sf.test.mongo;

import com.services.sf.commons.base.BaseAvroMapper;
import com.services.sf.mongo.test.avro.DummyEvent;
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
