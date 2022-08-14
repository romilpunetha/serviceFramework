package com.services.util.notification.producer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.services.common.domain.util.ContextData;
import com.services.common.domain.util.LocalContext;
import io.quarkus.arc.properties.IfBuildProperty;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

@IfBuildProperty(name = "framework.aws.sns.enabled", stringValue = "true")
public class BaseSNSProducer<T extends ContextData> {

    private final String topicArn;

    private final SnsAsyncClient snsAsyncClient;

    private final ObjectMapper objectMapper;

    @Inject
    protected LocalContext localContext;

    public BaseSNSProducer(String endpoint, String topicArn, Region region, Class<?> clazz) {
        this(endpoint, topicArn, region, clazz, new ObjectMapper().registerModule(new JavaTimeModule())
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL));
    }

    public BaseSNSProducer(String endpoint, String topicArn, Region region, Class<?> clazz, ObjectMapper objectMapper) {
        this.topicArn = topicArn;
        this.snsAsyncClient = SnsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(region)
                .build();
        this.objectMapper = objectMapper;
    }

    public Uni<Response> publish(T t, Map<String, MessageAttributeValue> messageAttributes) {
        t.setLocalContext(LocalContext.builder().context(localContext.getContext()).build());
        return Uni.createFrom().completionStage(
                        this.snsAsyncClient.publish(Unchecked.consumer(m -> {
                            m.topicArn(this.topicArn)
                                    .message(objectMapper.writeValueAsString(t))
                                    .messageAttributes(messageAttributes);
                        })))
                .map(publishResponse -> Response.ok().entity(publishResponse.messageId()).build())
                ;
    }
}
