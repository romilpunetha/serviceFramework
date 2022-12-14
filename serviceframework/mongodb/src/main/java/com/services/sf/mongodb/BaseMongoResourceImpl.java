package com.services.sf.mongodb;

import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.commons.base.BaseResourceImpl;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public class BaseMongoResourceImpl<T extends BaseMongoEntity, E extends BaseMongoDomain>
        extends BaseResourceImpl
        implements BaseMongoResource<T, E> {

}
