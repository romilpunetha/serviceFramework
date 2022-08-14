package com.services.sf.domain.basemongo;

import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.domain.base.BaseCacheMapper;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public interface BaseMongoCacheMapper<E extends BaseMongoDomain, C>
        extends BaseCacheMapper<E, C> {

}
