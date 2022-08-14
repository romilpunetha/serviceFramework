package com.services.util.lock;

import io.smallrye.mutiny.Uni;

public interface Lock {

    Uni<Void> acquire();

    Uni<Boolean> tryAcquire(Long attemptDurationInMilli);

    Uni<Void> release();

}
