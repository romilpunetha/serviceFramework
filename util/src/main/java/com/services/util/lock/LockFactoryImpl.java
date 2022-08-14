package com.services.util.lock;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.eclipse.microprofile.config.ConfigProvider;
import org.redisson.Redisson;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
@IfBuildProperty(name = "framework.redis.enabled", stringValue = "true")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LockFactoryImpl implements LockFactory {

    final Long defaultLeaseTimeInMillis = 30 * 1000L;
    final RedissonReactiveClient redissonClient;

    public LockFactoryImpl() {
        String clientType = ConfigProvider.getConfig().getOptionalValue("quarkus.redis.client-type", String.class).orElse("standalone").toLowerCase(Locale.ROOT);
        List<String> redisHosts = Arrays.stream(ConfigProvider.getConfig().getOptionalValue("quarkus.redis.hosts", String.class).orElse("redis://localhost:6379").split(",")).collect(Collectors.toList());
        Log.info(String.format("REDIS HOSTS : %s and type : %s", redisHosts, clientType));
        Config config = new Config();
        if ("cluster".equalsIgnoreCase(clientType)) {
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            for (String hosts : redisHosts)
                clusterServersConfig.addNodeAddress(hosts);
            // /3 because 3 replicas per shard exist
        } else {
            config.useSingleServer().setAddress(redisHosts.get(0));
        }
        this.redissonClient = Redisson.create(config).reactive();
    }

    public Lock getLock(final String lockName) {
        return this.getLock(lockName, defaultLeaseTimeInMillis);
    }

    public Lock getLock(final String lockName, final Long leaseTimeInMillis) {

        return DefaultLock.builder()
                .lock(redissonClient.getLock(lockName))
                .leaseTimeInMillis(leaseTimeInMillis)
                .build();
    }

    public Lock getMultiLock(@NonNull final List<String> lockNames) {
        return this.getMultiLock(lockNames, defaultLeaseTimeInMillis);
    }

    public Lock getMultiLock(@NonNull final List<String> lockNames, final Long leaseTimeInMillis) {
        return MultiLock
                .builder()
                .leaseTimeInMillis(leaseTimeInMillis)
                .lock(redissonClient.getMultiLock(getLockArray(lockNames).toArray(new RLockReactive[0])))
                .build();
    }

    private List<RLockReactive> getLockArray(List<String> lockNames) {

        List<RLockReactive> rLocks = new ArrayList<>();

        lockNames.stream().sorted().distinct().forEach(lockName -> {
            rLocks.add(redissonClient.getLock(lockName));
        });
        return rLocks;
    }

}
