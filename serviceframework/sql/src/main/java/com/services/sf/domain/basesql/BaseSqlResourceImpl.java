package com.services.sf.domain.basesql;

import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.sf.domain.base.BaseResourceImpl;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class BaseSqlResourceImpl<T extends BaseSqlEntity, E extends BaseSqlDomain>
        extends BaseResourceImpl
        implements BaseSqlResource<T, E> {

}
