package com.services.sf.test.dummy;

import com.services.sf.domain.basesql.BaseSqlMapper;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "cdi",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DummyMapper extends BaseSqlMapper<DummyEntity, Dummy> {
}