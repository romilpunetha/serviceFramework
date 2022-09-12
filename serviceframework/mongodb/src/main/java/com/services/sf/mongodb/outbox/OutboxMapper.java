package com.services.sf.mongodb.outbox;

import com.services.common.domain.basemongo.Outbox;
import com.services.sf.mongodb.BaseMongoMapper;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "cdi",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        imports = ObjectId.class)
public interface OutboxMapper extends BaseMongoMapper<OutboxEntity, Outbox> {
}
