package com.services.sf.domain.basemongo;

import com.services.common.domain.basemongo.BaseMongoDomain;
import com.services.sf.domain.base.BaseDomainCacheRepositoryImpl;
import io.quarkus.arc.properties.IfBuildProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
@IfBuildProperty(name = "framework.redis.enabled", stringValue = "true")
@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public class BaseMongoCacheRepositoryImpl<E extends BaseMongoDomain, C>
        extends BaseDomainCacheRepositoryImpl<E, C>
        implements BaseMongoCacheRepository<E, C> {

    public BaseMongoCacheRepositoryImpl(@NotNull BaseMongoCacheMapper<E, C> mapper,
                                        @NotNull Class<C> domainClass,
                                        @NotNull String bucketPrefix) {
        super(mapper, domainClass, bucketPrefix);
    }

    public BaseMongoCacheRepositoryImpl(@NotNull BaseMongoCacheMapper<E, C> mapper,
                                        @NotNull Class<C> domainClass,
                                        @NotNull String bucketPrefix,
                                        @NotNull Boolean hasTenant) {

        super(mapper, domainClass, bucketPrefix, hasTenant);
    }
}

