package com.services.util.event;

import com.services.common.domain.util.Context;
import com.services.common.domain.util.ContextData;
import com.services.common.domain.util.LocalContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.ObjectUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseEventEmitterImpl
        implements EventEmitter {

    final DeliveryOptions deliveryOptions = new DeliveryOptions().setTracingPolicy(TracingPolicy.ALWAYS);
    @Inject
    protected LocalContext localContext;
    @Inject
    protected EventBus eventBus;

    public <T extends ContextData> void publish(String eventName, T eventData) {

        merge(eventData);
        eventBus.publish(eventName, eventData, deliveryOptions);
    }

    public <T extends ContextData, E> Uni<E> request(String eventName, T eventData) {

        merge(eventData);
        return eventBus.<E>request(eventName, eventData, deliveryOptions)
                .map(Message::body);
    }

    public <T extends ContextData> void requestAndForget(String eventName, T eventData) {

        merge(eventData);
        eventBus.requestAndForget(eventName, eventData, deliveryOptions);
    }

    private <T extends ContextData> void merge(T eventData) {

        if (ObjectUtils.isEmpty(eventData.getLocalContext())) {
            eventData.setLocalContext(LocalContext.builder().build());
        }

        Context context = localContext.getContext();

        context.getEntries().putAll(eventData.getLocalContext().getContext().getEntries());

        eventData.getLocalContext().setContext(context);
    }

}
