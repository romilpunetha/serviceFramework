package com.services.sf.domain.basesql;

import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.sf.domain.base.BaseDomainCacheRepositoryImpl;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
@IfBuildProperty(name = "framework.redis.enabled", stringValue = "true")
public class BaseSqlCacheRepositoryImpl<E extends BaseSqlDomain, C>
        extends BaseDomainCacheRepositoryImpl<E, C>
        implements BaseSqlCacheRepository<E, C> {
}