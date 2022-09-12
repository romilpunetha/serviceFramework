package com.services.sf.commons.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.sf.commons.abstracts.AbstractEntity;
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
public abstract class BaseEntity<ID> extends AbstractEntity<ID> {

    ID id;

    String tenantId;

    String isTestData;

    Instant createdAt;

    Instant lastModifiedAt;

    Long version;

    Instant deletedAt;

    String createdBy;

}
