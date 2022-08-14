package com.services.util.lock;

import lombok.NonNull;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public interface LockFactory {

    Lock getLock(@NotBlank final String lockName);

    Lock getLock(@NotBlank final String lockName, @NotNull final Long leaseTimeInMillis);

    Lock getMultiLock(@NonNull final List<String> lockNames);

    Lock getMultiLock(@NonNull final List<String> lockNames, @NotNull final Long leaseTimeInMillis);
}
