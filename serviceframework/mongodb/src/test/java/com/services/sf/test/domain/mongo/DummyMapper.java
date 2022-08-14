package com.services.sf.test.domain.mongo;

import com.services.sf.domain.basemongo.BaseMongoMapper;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "cdi",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        imports = ObjectId.class)
public interface DummyMapper extends BaseMongoMapper<DummyEntity, Dummy> {
}
