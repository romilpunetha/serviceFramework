package com.services.sf.mongodb;

import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.commons.base.BaseDomainCacheRepository;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
@IfBuildProperty(name = "framework.redis.enabled", stringValue = "true")
public interface BaseMongoCacheRepository<E extends BaseMongoDomain, C>
        extends BaseDomainCacheRepository<E, C> {

}