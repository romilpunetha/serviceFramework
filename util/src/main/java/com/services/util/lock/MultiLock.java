package com.services.util.lock;

import io.opentelemetry.extension.annotations.WithSpan;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.redisson.api.RLockReactive;

import javax.validation.constraints.NotNull;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MultiLock implements Lock {

    @NotNull
    Long leaseTimeInMillis;

    @Builder.Default
    Long lockId = new Random().nextLong();

    RLockReactive lock;

    @WithSpan
    public Uni<Void> acquire() {
        return Uni.createFrom().publisher(lock.lock(leaseTimeInMillis, TimeUnit.MILLISECONDS, lockId));
    }

    @WithSpan
    public Uni<Boolean> tryAcquire(Long attemptDurationInMilli) {
        return Uni.createFrom().publisher(lock.tryLock(attemptDurationInMilli, leaseTimeInMillis, TimeUnit.MILLISECONDS, lockId));

    }

    @WithSpan
    public Uni<Void> release() {
        return Uni.createFrom().publisher(lock.unlock(lockId));
    }
}
