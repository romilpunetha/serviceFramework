package com.services.sf.clickhouse;

import com.services.common.domain.base.BaseDomain;
import com.services.sf.commons.base.BaseEntity;
import com.services.sf.commons.base.BaseRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface BaseClickHouseRepository<T extends BaseEntity<ID>, E extends BaseDomain, ID> extends BaseRepository<T, E, ID> {

    Uni<E> findOne(String query);

    Multi<E> findAll(String query);
}
