package com.services.sf.mongodb;

import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.commons.base.BaseResource;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public interface BaseMongoResource<T extends BaseMongoEntity, E extends BaseMongoDomain>
        extends BaseResource {
}
