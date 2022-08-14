package com.services.sf.domain.basesql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.sf.domain.base.BaseEntity;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

@SuperBuilder(toBuilder = true)
@Getter(onMethod_ = {@Override})
@Setter(onMethod_ = {@Override})
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@MappedSuperclass
public abstract class BaseSqlEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String tenantId;

    String isTestData;

    Instant createdAt;

    Instant lastModifiedAt;

    Long version;

    Instant deletedAt;

    String createdBy;

}
