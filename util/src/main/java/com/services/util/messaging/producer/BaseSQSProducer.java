package com.services.util.messaging.producer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.services.common.domain.util.ContextData;
import com.services.common.domain.util.LocalContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseSQSProducer<T extends ContextData> {

    @Inject
    protected LocalContext localContext;

    String endpoint;
    SqsAsyncClient sqsClient;
    ObjectMapper objectMapper;

    public BaseSQSProducer(String endpoint, Region region, Class<?> clazz) {
        this(endpoint, region, clazz, new ObjectMapper().registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL));
    }

    public BaseSQSProducer(String endpoint, Region region, Class<?> clazz, ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.sqsClient = SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .httpClient(NettyNioAsyncHttpClient.builder().build())
                .region(region)
                .build();
        this.objectMapper = objectMapper;
    }

    public Uni<Response> publish(T t, Map<String, MessageAttributeValue> messageAttributes) {
        return this.publish(t, 0, messageAttributes);
    }

    public Uni<Response> publish(T t, Integer delaySeconds, Map<String, MessageAttributeValue> messageAttributes) {
        t.setLocalContext(LocalContext.builder().context(localContext.getContext()).build());
        return Uni.createFrom().completionStage(
                        this.sqsClient.sendMessage(Unchecked.consumer(m -> m.queueUrl(this.endpoint)
                                .messageBody(this.objectMapper.writeValueAsString(t))
                                .delaySeconds(Math.min(delaySeconds, 900))
                                .messageAttributes(messageAttributes))))
                .map(sendMessageResponse -> Response.ok().entity(sendMessageResponse.messageId()).build())
                ;
    }

    public Uni<Response> changeMessageVisibility(String receiptHandle, Integer visibilityTimeoutInSeconds) {
        ChangeMessageVisibilityRequest changeMessageVisibilityRequest = ChangeMessageVisibilityRequest.builder()
                .queueUrl(this.endpoint)
                .receiptHandle(receiptHandle)
                .visibilityTimeout(visibilityTimeoutInSeconds)
                .build();

        return Uni.createFrom().completionStage(
                        this.sqsClient.changeMessageVisibility(changeMessageVisibilityRequest))
                .map(sendMessageResponse -> Response.ok().build());
    }
}
