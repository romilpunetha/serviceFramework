package com.services.sf.clickhouse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.services.sf.commons.base.BaseEntity;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;


@SuperBuilder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@IfBuildProperty(name = "framework.clickhouse.enabled", stringValue = "true")
public abstract class BaseClickHouseEntity<ID> extends BaseEntity<ID> {

    String schemaVersion;
}