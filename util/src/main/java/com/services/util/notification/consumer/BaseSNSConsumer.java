package com.services.util.notification.consumer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.services.common.domain.util.ContextData;
import com.services.common.domain.util.LocalContext;
import com.services.common.domain.util.SNSNotification;
import com.services.common.domain.util.SNSSubscriptionConfirmation;
import com.services.common.enums.ErrorCode;
import com.services.common.enums.ErrorLevel;
import com.services.common.exception.BaseRuntimeException;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.eclipse.microprofile.context.ManagedExecutor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@ActivateRequestContext
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.aws.sns.enabled", stringValue = "true")
public abstract class BaseSNSConsumer<T extends ContextData> {

    static final String NOTIFICATION_TYPE = "Notification";
    static final String SUBSCRIPTION_CONFIRMATION_TYPE = "SubscriptionConfirmation";
    static final String UNSUBSCRIBE_CONFIRMATION_TYPE = "UnsubscribeConfirmation";
    protected static Map<Class<?>, ObjectReader> READERS = new HashMap<>();

    final SnsAsyncClient snsAsyncClient;
    final String topicArn;
    final Class<T> clazz;
    @Inject
    protected ManagedExecutor managedExecutor;

    @Inject
    protected LocalContext localContext;

    volatile String subscriptionArn = null;

    public BaseSNSConsumer(String endpoint, String topicArn, Region region, Class<T> clazz) {
        READERS.put(SNSNotification.class, new ObjectMapper().readerFor(SNSNotification.class));
        READERS.put(SNSSubscriptionConfirmation.class, new ObjectMapper().readerFor(SNSSubscriptionConfirmation.class));
        READERS.put(clazz, new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .readerFor(clazz));

        this.snsAsyncClient = SnsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(region)
                .build();
        this.topicArn = topicArn;
        this.clazz = clazz;
    }

    public abstract void process(T t);

    public abstract String notificationEndpoint();

    public Uni<Response> consume(String messageType, String message) throws JsonProcessingException {
        switch (messageType) {
            case NOTIFICATION_TYPE: {
                Uni.createFrom().item(readObject(SNSNotification.class, message))
                        .map(Unchecked.function(snsNotification -> readObject(this.clazz, snsNotification.getMessage())))
                        .invoke(t -> localContext.setContext(t.getLocalContext().getContext()))
                        .runSubscriptionOn(this.managedExecutor)
                        .subscribe()
                        .with(this::process)
                ;
                return Uni.createFrom().item(Response.ok().build());
            }
            case SUBSCRIPTION_CONFIRMATION_TYPE: {
                return Uni.createFrom().item(readObject(SNSSubscriptionConfirmation.class, message))
                        .chain(msg -> Uni.createFrom().completionStage(
                                this.snsAsyncClient.confirmSubscription(confirm -> confirm.topicArn(this.topicArn).token(msg.getToken())))
                        )
                        .invoke(resp -> Log.info("Subscription confirmed for topic :" + this.topicArn))
                        .map(resp -> Response.ok().build());
            }
            case UNSUBSCRIBE_CONFIRMATION_TYPE: {
                Log.info("Unsubscribed from topic : " + this.topicArn);
                return Uni.createFrom().item(Response.ok().build());
            }
            default: {
                throw new BaseRuntimeException(
                        ErrorLevel.ERROR,
                        ErrorCode.BAD_REQUEST,
                        "Incorrect Message Type",
                        "MessageType is null or unknown : " + messageType
                );
            }
        }
    }

    public Uni<Response> subscribe() {
        return Uni.createFrom()
                .completionStage(this.snsAsyncClient.subscribe(s -> s.topicArn(topicArn).protocol("http").endpoint(notificationEndpoint())))
                .map(SubscribeResponse::subscriptionArn)
                .invoke(this::setSubscriptionArn)
                .invoke(arn -> Log.info("Subscribed " + this.topicArn + " with id : " + this.subscriptionArn))
                .map(arn -> Response.ok().entity(arn).build());
    }

    public Uni<Response> unsubscribe() {
        if (this.subscriptionArn != null) {
            return Uni.createFrom()
                    .completionStage(this.snsAsyncClient.unsubscribe(s -> s.subscriptionArn(subscriptionArn)))
                    .invoke(unsubscribeResponse -> Log.info("Unsubscribed " + this.topicArn + " for id : " + this.subscriptionArn))
                    .invoke(unsubscribeResponse -> subscriptionArn = null)
                    .map(unsubscribeResponse -> Response.ok().build());
        } else {
            return Uni.createFrom().item(Response.status(400).entity("Not subscribed yet").build());
        }
    }

    private void setSubscriptionArn(String arn) {
        this.subscriptionArn = arn;
    }

    private <K> K readObject(Class<K> clazz, String message) throws JsonProcessingException {
        return READERS.get(clazz).readValue(message);
    }

}
