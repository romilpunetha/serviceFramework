package com.services.sf.domain.basesql;

import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.sf.domain.base.BaseRepository;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BaseSqlRepository<T extends BaseSqlEntity, E extends BaseSqlDomain>
        extends PanacheRepository<T>,
        BaseRepository<T, E, Long> {
    @Override
    @Deprecated
    default PanacheQuery<T> find(String query, Object... params) {
        return PanacheRepository.super.find(query, params);
    }

    @Override
    @Deprecated
    default PanacheQuery<T> find(String query, Map<String, Object> params) {
        return PanacheRepository.super.find(query, params);
    }

    @Override
    @Deprecated
    default PanacheQuery<T> find(String query, Parameters params) {
        return PanacheRepository.super.find(query, params);
    }

    @Override
    @Deprecated
    default PanacheQuery<T> find(String query, Sort sort, Object... params) {
        return PanacheRepository.super.find(query, sort, params);
    }

    @Override
    @Deprecated
    default PanacheQuery<T> find(String query, Sort sort, Map<String, Object> params) {
        return PanacheRepository.super.find(query, sort, params);
    }

    @Override
    @Deprecated
    default PanacheQuery<T> find(String query, Sort sort, Parameters params) {
        return PanacheRepository.super.find(query, sort, params);
    }

    @Override
    @Deprecated
    default Uni<T> findById(Long aLong) {
        return PanacheRepository.super.findById(aLong);
    }

    @Override
    @Deprecated
    default PanacheQuery<T> findAll() {
        return PanacheRepository.super.findAll();
    }

    @Override
    @Deprecated
    default PanacheQuery<T> findAll(Sort sort) {
        return PanacheRepository.super.findAll(sort);
    }

    Uni<T> getById(Long id);

    Set<String> getAllowedSortFields();

    @Override
    @Deprecated
    Multi<String> bulkCreate(List<E> es);
}
