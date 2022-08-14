package com.services.util.event;

import com.services.common.domain.util.ContextData;
import io.smallrye.mutiny.Uni;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public interface EventEmitter {

    <T extends ContextData> void publish(@NotBlank String eventName, @NotNull T eventData);

    <T extends ContextData, E> Uni<E> request(@NotBlank String address, @NotNull T eventData);

    <T extends ContextData> void requestAndForget(@NotBlank String address, @NotNull T eventData);
}
