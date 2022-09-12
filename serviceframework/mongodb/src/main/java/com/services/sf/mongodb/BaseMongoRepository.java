package com.services.sf.mongodb;

import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.base.BulkResponse;
import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.commons.base.BaseRepository;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;


@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public interface BaseMongoRepository<T extends BaseMongoEntity, E extends BaseMongoDomain>
        extends ReactivePanacheMongoRepository<T>,
        BaseRepository<T, E, ObjectId> {

    default Uni<E> delete(E e) {
        return this.delete(e, null);
    }

    Uni<E> get(@NotBlank String id, ClientSession clientSession);

    Uni<E> create(@NotNull E e, ClientSession clientSession);

    Uni<E> delete(@NotNull E e, ClientSession clientSession);

    Uni<E> upsert(@NotNull E e, E filter, ClientSession clientSession);

    Multi<E> getByIds(@Size(min = 1) List<String> ids, List<String> sortOrder, ClientSession clientSession);

    Uni<E> patch(@NotNull E e, ClientSession clientSession);

    Uni<E> put(@NotNull E e, ClientSession clientSession);

    Multi<String> bulkCreate(@NotEmpty List<E> eList, ClientSession clientSession);

    Uni<Void> bulkPatch(@NotEmpty List<E> eList, ClientSession clientSession);

    Uni<BulkResponse> bulkUpsert(@NotEmpty List<E> eList, ClientSession clientSession);

    Uni<E> findOne(Document query);

    Uni<E> findOne(Document query, ClientSession clientSession);

    Uni<E> findOne(Document filter, FindOptions findOptions, ClientSession clientSession);

    Uni<Boolean> updateOne(Document filter, Document update, ClientSession clientSession);

    Uni<Boolean> updateOne(Document filter, Document update, UpdateOptions updateOptions, ClientSession clientSession);

    Multi<E> findAll(Document query);

    Multi<E> findAll(Document query, FindOptions findOptions);

    Multi<E> findAll(Document query, List<String> sortOrder, Integer offset, Integer limit);

    Multi<E> findAll(Document filter, FindOptions findOptions, ClientSession clientSession);

    Uni<E> findOneAndUpdate(Document filter, Document update, FindOneAndUpdateOptions options, ClientSession clientSession);

    Uni<E> findOneAndReplace(Document filter,
                             E replace,
                             FindOneAndReplaceOptions options,
                             ClientSession clientSession);

    Uni<List<E>> getList(Document query);

    Uni<Long> countDocuments(Document filter);

    Uni<BulkResponse> bulkWrite(List<UpdateManyModel<E>> eList, ClientSession clientSession);

    Multi<E> aggregate(List<Document> filters, ClientSession clientSession);

    <D> Multi<D> aggregate(List<Document> filters, Class<D> clazz, ClientSession clientSession);
}
