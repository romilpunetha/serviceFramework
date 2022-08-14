package com.services.sf.domain.basesql.outbox;

import com.services.common.domain.basesql.Outbox;
import com.services.sf.domain.basesql.BaseSqlMapper;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "cdi",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OutboxMapper extends BaseSqlMapper<OutboxEntity, Outbox> {
}
