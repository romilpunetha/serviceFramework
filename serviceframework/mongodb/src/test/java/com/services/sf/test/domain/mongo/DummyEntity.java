package com.services.sf.test.domain.mongo;

import com.services.sf.domain.basemongo.BaseMongoEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@MongoEntity(collection = "testCollection")
public class DummyEntity extends BaseMongoEntity {

    String hello;

    Integer a;

    Integer b;

    Instant time;
}
