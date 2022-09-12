package com.services.sf.mongodb;

import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.abstracts.AbstractDomain;
import com.services.common.domain.base.BulkResponse;
import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.commons.base.BaseService;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;

import java.util.List;

@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public interface BaseMongoService<T extends BaseMongoEntity, E extends BaseMongoDomain, C extends AbstractDomain>
        extends BaseService<T, E, C, ObjectId> {

    Uni<E> get(String id, ClientSession clientSession);

    Uni<E> getSecondaryPreferred(String id);

    Uni<E> create(E e, ClientSession clientSession);

    Uni<E> patch(String id, E e, ClientSession clientSession);

    Uni<E> upsert(E e, E filter, ClientSession clientSession);

    Uni<E> put(String id, E e, ClientSession clientSession);

    Uni<E> delete(E e, ClientSession clientSession);

    Multi<String> bulkCreate(List<E> eList, ClientSession clientSession);

    Uni<Void> bulkPatch(List<E> eList, ClientSession clientSession);

    Uni<BulkResponse> bulkUpsert(List<E> eList, ClientSession clientSession);

    Multi<E> getByIdsSecondaryPreferred(List<String> ids, List<String> sortOrder);

}
