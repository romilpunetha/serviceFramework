package com.services.sf.commons.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.services.common.domain.base.BaseDomain;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import org.jboss.marshalling.Pair;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@IfBuildProperty(name = "framework.redis.enabled", stringValue = "true")
public interface BaseDomainCacheRepository<E extends BaseDomain, C> {

    Uni<Boolean> exists(@NotBlank final String id);

    Uni<E> get(@NotBlank final String id);

    Multi<Optional<E>> get(@NotEmpty final List<String> ids);

    Uni<List<E>> getList(@NotBlank String id);

    Uni<Response> set(@NotBlank final String id,
                      @NotNull final E t) throws JsonProcessingException;

    Uni<Response> set(@NotBlank final String id,
                      @NotNull final E t,
                      Long expiryInMilliseconds) throws JsonProcessingException;

    Uni<Response> set(final String id,
                      final List<E> t) throws JsonProcessingException;

    Uni<Response> set(final String id,
                      final List<E> t,
                      Long expiryInMilliseconds) throws JsonProcessingException;

    Uni<Void> delete(@NotEmpty final List<String> ids);

    Uni<Void> mset(@NotNull final Map<String, E> mp);

    Uni<Response> hset(@NotBlank final String hash,
                       @NotBlank String field,
                       @NotNull final E e) throws JsonProcessingException;

    Uni<Response> hset(@NotBlank final String hash,
                       @NotBlank String field,
                       @NotNull final E e,
                       Long timeInSeconds) throws JsonProcessingException;

    Uni<Response> hset(@NotBlank final String hash,
                       @NotBlank String field,
                       @NotNull final E e,
                       Instant instant) throws JsonProcessingException;

    Uni<Response> hmset(final String hash,
                        final List<String> key,
                        final List<E> values);

    Uni<Response> hmset(final String hash,
                        final List<String> key,
                        final List<E> values,
                        Long timeInSeconds);

    Uni<Response> hmset(final String hash,
                        final List<String> key,
                        final List<E> values,
                        Instant instant);

    Uni<E> hget(@NotBlank final String hash,
                @NotBlank final String field);

    Uni<Map<String, E>> hgetall(@NotBlank final String hash);

    Uni<Void> hdel(@NotBlank final String hash,
                   @NotEmpty final List<String> fields);

    Multi<String> hkeys(@NotBlank final String hash);

    Multi<Optional<E>> hmget(@NotBlank final String hash,
                             @NotEmpty final List<String> fields);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty Map<E, Number> map);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty Map<E, Number> map,
                       Long timeInSeconds);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty Map<E, Number> map,
                       Instant instant);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty List<E> values,
                       @NotEmpty List<Number> scores);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty List<E> values,
                       @NotEmpty List<Number> scores,
                       Long timeInSeconds);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty List<E> values,
                       @NotEmpty List<Number> scores,
                       Instant instant);

    Uni<Pair<E, Double>> zpopmax(@NotBlank String hash);

    Uni<Pair<E, Double>> zpopmin(@NotBlank String hash);

    Multi<Optional<E>> zrange(@NotBlank String hash, Integer start, Integer end);

    Multi<Optional<E>> zrevrange(@NotBlank String hash, Integer start, Integer end);

    Uni<Response> expire(@NotBlank String key,
                         Long timeInSeconds);

    Uni<Response> expireat(@NotBlank String key,
                           Instant instant);
}