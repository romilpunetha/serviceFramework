package com.services.sf.sql;

import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.sf.commons.base.BaseResourceImpl;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class BaseSqlResourceImpl<T extends BaseSqlEntity, E extends BaseSqlDomain>
        extends BaseResourceImpl
        implements BaseSqlResource<T, E> {

}
