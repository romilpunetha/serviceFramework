package com.services.sf.domain.base;

import com.services.common.domain.base.BaseDomain;
import com.services.common.domain.basemongo.BulkResponse;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public interface BaseRepository<T extends BaseEntity<ID>, E extends BaseDomain, ID> {
    Uni<E> create(@NotNull E e);

    Uni<E> upsert(@NotNull E e, E filter);

    Uni<E> get(@NotNull String id);

    Multi<E> getByIds(@Size(min = 1) List<String> ids, List<String> sortOrder);

    Uni<E> patch(@NotNull E e);

    Uni<E> put(@NotNull E e);

    Multi<String> bulkCreate(@NotEmpty List<E> eList);

    Uni<List<E>> bulkCreateWithResponse(@NotEmpty List<E> eList);

    Uni<Void> bulkPatch(@NotEmpty List<E> eList);

    Uni<BulkResponse> bulkUpsert(@NotEmpty List<E> eList);

    Uni<E> delete(E e);

    Multi<E> findByPage(@Min(0) Integer offset,
                        @Min(1) Integer limit,
                        List<String> sortOrder);

    Multi<E> findByCreatedAtGreaterThan(@NotNull Instant t1,
                                        @Min(0) Integer offset,
                                        @Min(1) Integer limit,
                                        List<String> sortOrder);

    Multi<E> findByCreatedAtLessThan(@NotNull Instant t1,
                                     @Min(0) Integer offset,
                                     @Min(1) Integer limit,
                                     List<String> sortOrder);

    Multi<E> findByCreatedAtBetween(@NotNull Instant t1,
                                    @NotNull Instant t2,
                                    @Min(0) Integer offset,
                                    @Min(1) Integer limit,
                                    List<String> sortOrder);

    Multi<E> findByLastModifiedAtGreaterThan(@NotNull Instant t1,
                                             @Min(0) Integer offset,
                                             @Min(1) Integer limit,
                                             List<String> sortOrder);

    Multi<E> findByLastModifiedAtLessThan(@NotNull Instant t1,
                                          @Min(0) Integer offset,
                                          @Min(1) Integer limit,
                                          List<String> sortOrder);

    Multi<E> findByLastModifiedAtBetween(@NotNull Instant t1,
                                         @NotNull Instant t2,
                                         @Min(0) Integer offset,
                                         @Min(1) Integer limit,
                                         List<String> sortOrder);

    Multi<E> findByLastDeletedAtGreaterThan(@NotNull Instant t1,
                                            @Min(0) Integer offset,
                                            @Min(1) Integer limit,
                                            List<String> sortOrder);

    Multi<E> findByDeletedAtLessThan(@NotNull Instant t1,
                                     @Min(0) Integer offset,
                                     @Min(1) Integer limit,
                                     List<String> sortOrder);

    Multi<E> findByDeletedAtBetween(@NotNull Instant t1,
                                    @NotNull Instant t2,
                                    @Min(0) Integer offset,
                                    @Min(1) Integer limit,
                                    List<String> sortOrder);
}
