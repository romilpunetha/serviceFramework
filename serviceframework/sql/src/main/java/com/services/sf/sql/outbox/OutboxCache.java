package com.services.sf.sql.outbox;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.common.domain.abstracts.AbstractDomain;
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
public class OutboxCache extends AbstractDomain {

    String aggregateId;
    String aggregateType;
    String artifactId;
    String groupId;
    byte[] payload;
    String eventType;
    Instant expireAt;
    Instant eventOccurredAt;
    Map<String, Object> additionalFieldValues;
}
