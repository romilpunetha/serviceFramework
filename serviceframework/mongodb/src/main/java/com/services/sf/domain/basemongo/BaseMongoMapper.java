package com.services.sf.domain.basemongo;

import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.domain.base.BaseMapper;
import io.quarkus.arc.properties.IfBuildProperty;
import org.bson.types.ObjectId;

@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public interface BaseMongoMapper<T extends BaseMongoEntity, E extends BaseMongoDomain>
        extends BaseMapper<T, E> {

    default String toString(ObjectId id) {
        return id.toString();
    }

    default ObjectId toObjectId(String id) {
        return new ObjectId(id);
    }
}
