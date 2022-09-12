package com.services.sf.clickhouse;

import com.services.common.baseclickhouse.BaseClickhouseDomain;
import com.services.sf.commons.base.BaseMapper;
import io.quarkus.arc.properties.IfBuildProperty;

@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public interface BaseClickHouseMapper<T extends BaseClickHouseEntity<ID>, E extends BaseClickhouseDomain, ID>
        extends BaseMapper<T, E> {

}
