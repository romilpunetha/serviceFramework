package com.services.sf.domain.basesql;

import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.sf.domain.base.BaseMapper;

public interface BaseSqlMapper<T extends BaseSqlEntity, E extends BaseSqlDomain>
        extends BaseMapper<T, E> {

    default String toString(Long id) {
        return String.valueOf(id);
    }

    default Long toLong(String id) {
        return Long.parseLong(id);
    }
}
