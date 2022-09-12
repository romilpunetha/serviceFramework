package com.services.sf.commons.base;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public interface BaseCacheRepository<C> {

    Uni<Boolean> exists(@NotBlank final String id);

    Uni<C> get(@NotBlank final String id);

    Multi<Optional<C>> get(@NotEmpty final List<String> ids);

    Uni<List<C>> getList(@NotBlank String id);

    Uni<Response> set(@NotBlank final String id,
                      @NotNull final C t) throws JsonProcessingException;

    Uni<Response> set(@NotBlank final String id,
                      @NotNull final C t,
                      Long expiryInMilliseconds) throws JsonProcessingException;

    Uni<Response> set(final String id,
                      final List<C> t) throws JsonProcessingException;

    Uni<Response> set(final String id,
                      final List<C> t,
                      Long expiryInMilliseconds) throws JsonProcessingException;

    Uni<Void> delete(@NotEmpty final List<String> ids);

    Uni<Void> mset(@NotNull final Map<String, C> mp);

    Uni<Response> hset(@NotBlank final String hash,
                       @NotBlank String field,
                       @NotNull final C e) throws JsonProcessingException;

    Uni<Response> hset(@NotBlank final String hash,
                       @NotBlank String field,
                       @NotNull final C e,
                       Long timeInSeconds) throws JsonProcessingException;

    Uni<Response> hset(@NotBlank final String hash,
                       @NotBlank String field,
                       @NotNull final C e,
                       Instant instant) throws JsonProcessingException;

    Uni<Response> hmset(final String hash,
                        final List<String> key,
                        final List<C> values);

    Uni<Response> hmset(final String hash,
                        final List<String> key,
                        final List<C> values,
                        Long timeInSeconds);

    Uni<Response> hmset(final String hash,
                        final List<String> key,
                        final List<C> values,
                        Instant instant);

    Uni<C> hget(@NotBlank final String hash,
                @NotBlank final String field);

    Uni<Map<String, C>> hgetall(@NotBlank final String hash);

    Uni<Void> hdel(@NotBlank final String hash,
                   @NotEmpty final List<String> fields);

    Multi<String> hkeys(@NotBlank final String hash);

    Multi<Optional<C>> hmget(@NotBlank final String hash,
                             @NotEmpty final List<String> fields);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty Map<C, Number> map);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty Map<C, Number> map,
                       Long timeInSeconds);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty Map<C, Number> map,
                       Instant instant);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty List<C> values,
                       @NotEmpty List<Number> scores);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty List<C> values,
                       @NotEmpty List<Number> scores,
                       Long timeInSeconds);

    Uni<Response> zadd(@NotBlank String hash,
                       @NotEmpty List<C> values,
                       @NotEmpty List<Number> scores,
                       Instant instant);

    Uni<Pair<C, Double>> zpopmax(@NotBlank String hash);

    Uni<Pair<C, Double>> zpopmin(@NotBlank String hash);

    Multi<Optional<C>> zrange(@NotBlank String hash, Integer start, Integer end);

    Multi<Optional<C>> zrevrange(@NotBlank String hash, Integer start, Integer end);

    Uni<Response> expire(@NotBlank String key,
                         Long timeInSeconds);

    Uni<Response> expireat(@NotBlank String key,
                           Instant instant);
}