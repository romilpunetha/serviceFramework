package com.services.sf.domain.basesql;

import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.basemongo.BulkResponse;
import com.services.common.domain.basesql.BaseSqlDomain;
import com.services.sf.domain.base.BaseService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface BaseSqlService<T extends BaseSqlEntity, E extends BaseSqlDomain, C extends AbstractDomain>
        extends BaseService<T, E, C, Long> {

    Uni<E> get(String id);

    Uni<E> create(E e);

    Uni<E> patch(String id, E e);

    Uni<E> upsert(E e, E filter);

    Uni<E> put(String id, E e);

    Uni<E> delete(E e);

    @Deprecated
    Multi<String> bulkCreate(List<E> eList);

    Uni<List<E>> bulkCreateWithResponse(List<E> es);

    Uni<Void> bulkPatch(List<E> eList);

    Uni<BulkResponse> bulkUpsert(List<E> eList);
}
