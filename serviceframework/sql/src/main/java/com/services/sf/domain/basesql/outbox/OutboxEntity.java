package com.services.sf.domain.basesql.outbox;

import com.services.sf.domain.basesql.BaseSqlEntity;
import com.services.sf.domain.basesql.MapJsonConverter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import javax.persistence.Convert;
import javax.persistence.Entity;
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
@Entity
public class OutboxEntity extends BaseSqlEntity {

    String aggregateId;
    String aggregateType;
    String artifactId;
    String groupId;
    byte[] payload;
    String eventType;
    Instant expireAt;
    Instant eventOccurredAt;
    @Convert(converter = MapJsonConverter.class)
    Map<String, Object> additionalFieldValues;


}