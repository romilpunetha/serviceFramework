package com.services.sf.domain.base;

import com.services.common.domain.base.BaseDomain;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseRepositoryImpl<T extends BaseEntity<ID>, E extends BaseDomain, ID> implements BaseRepository<T, E, ID> {

}
