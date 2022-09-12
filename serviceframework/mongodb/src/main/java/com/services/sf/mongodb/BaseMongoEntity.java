package com.services.sf.mongodb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.sf.commons.base.BaseEntity;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;

/**
 * MongoDb library does not allow generics beyond 1 level of inheritance. If the AbstractEntity has
 * the generic ID field, BaseEntity is at level 1 and BaseMongoEntity is at level 2 and any inherited
 * class such as UserEntity at level 3. So the `type` of the id field collapses from the generic
 * type ID to Object which Mongo's POJOCODEC is unable to decode.
 * <p>
 * In order to avoid this, ObjectId has been mandated as the ID field in BaseMongoEntity which allows
 * for the id field in the BaseEntity to be generic and accommodate all BaseServer functions in a
 * single place. Else, the id field cannot be part of BaseEntity and generic functions requiring id
 * such as cache invalidation would need to be pushed to BaseMongoServer.
 */

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public abstract class BaseMongoEntity extends BaseEntity<ObjectId> {

    String schemaVersion;
}