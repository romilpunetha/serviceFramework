package com.services.sf.mongodb.outbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.sf.mongodb.BaseMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@MongoEntity(collection = "outbox")
public class OutboxEntity extends BaseMongoEntity {

    String aggregateId;
    String aggregateType;
    String artifactId;
    String groupId;
    byte[] payload;
    String eventType;
    Map<String, Object> additionalFieldValues;
    Instant expireAt;
    Instant eventOccurredAt;
}