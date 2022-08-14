package com.services.common.domain.basesql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class Outbox extends BaseSqlDomain {

    String aggregateId;
    String aggregateType;
    byte[] payload;
    String eventType;
    Map<String, Object> additionalFieldValues;
    Instant expireAt;
    Instant eventOccurredAt;
}
