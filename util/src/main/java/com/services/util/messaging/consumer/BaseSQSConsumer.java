package com.services.util.messaging.consumer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.services.common.domain.util.ContextData;
import com.services.util.messaging.configuration.QueueConfiguration;
import io.opentelemetry.extension.annotations.WithSpan;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Vertx;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor
@ApplicationScoped
@ActivateRequestContext
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseSQSConsumer<T extends ContextData> {

    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Inject
    protected Vertx vertx;
    SqsAsyncClient sqsClient;

    @Getter
    ObjectReader objectReader;
    Long waitTimeInMilliSeconds;
    Boolean isBatchReady = true;
    Boolean isConsumerRunning = false;
    Boolean isBlocking = false;
    ReceiveMessageRequest receiveMessageRequest;

    public BaseSQSConsumer(String endpoint, Region region, Long waitTimeInMilliSeconds, Integer batchSize, Class<T> clazz, Boolean isBlocking) {
        this(endpoint, region, waitTimeInMilliSeconds, batchSize, null, clazz, isBlocking,
                new ObjectMapper().registerModule(new JavaTimeModule())
                        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        );
    }

    public BaseSQSConsumer(String endpoint, Region region, Long waitTimeInMilliSeconds, Integer batchSize, Class<T> clazz, Boolean isBlocking, ObjectMapper objectMapper) {
        this(endpoint, region, waitTimeInMilliSeconds, batchSize, null, clazz, isBlocking, objectMapper);
    }

    public BaseSQSConsumer(String endpoint, Region region, Long waitTimeInMilliSeconds, Integer batchSize, Integer receiveMessageWaitTimeInSec, Class<T> clazz, Boolean isBlocking, ObjectMapper objectMapper) {

        QueueConfiguration.enableQueue(endpoint);

        this.waitTimeInMilliSeconds = waitTimeInMilliSeconds;

        this.receiveMessageRequest = ReceiveMessageRequest.builder()
                .maxNumberOfMessages(batchSize)
                .queueUrl(endpoint)
                .build();

        this.objectReader = objectMapper.readerFor(clazz);

        this.isBlocking = isBlocking;

        this.sqsClient = SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .httpClient(NettyNioAsyncHttpClient.builder().build())
                .region(region).build();

        Infrastructure.setDroppedExceptionHandler(err -> Log.error(err.getMessage(), err));

        if (batchSize > 0) {
            schedule();
        }
    }


    private void schedule() {
        isConsumerRunning = true;

        Callable<Void> fetchSQSMessages = new Callable<>() {
            @Override
            public Void call() {
                try {
                    if (isConsumerRunning) {
                        if (isConsumerEnabled()) {
                            Uni.createFrom()
                                    .completionStage(sqsClient.receiveMessage(receiveMessageRequest))
                                    .map(ReceiveMessageResponse::messages)
                                    .emitOn(isBlocking ? Infrastructure.getDefaultExecutor() : vertx.nettyEventLoopGroup())
                                    .subscribe()
                                    .with(messages -> consume(messages));
                        } else {
                            Log.info("Disabling consumer " + receiveMessageRequest.queueUrl() + " for " + waitTimeInMilliSeconds + " seconds.");
                        }
                    }
                } finally {
                    executor.schedule(this, getDelay(), TimeUnit.MILLISECONDS);
                }
                return null;
            }

        };
        executor.schedule(fetchSQSMessages, 0, TimeUnit.MILLISECONDS);
    }

    @WithSpan
    public abstract Uni<Boolean> process(Message message);

    private void consume(List<Message> messages) {
        if (!messages.isEmpty()) {
            isBatchReady = true;
            Multi.createFrom().iterable(messages)
                    .call(message -> process(message)
                            .invoke(aBoolean -> {
                                        if (aBoolean) delete(message);
                                    }
                            ))
                    .collect().asList().subscribeAsCompletionStage();
        } else {
            isBatchReady = false;
        }
    }

    private void delete(Message message) {
        this.sqsClient.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueUrl(this.receiveMessageRequest.queueUrl())
                        .receiptHandle(message.receiptHandle())
                        .build());
    }

    protected boolean isConsumerEnabled() {
        return QueueConfiguration.isConsumerEnabled(this.receiveMessageRequest.queueUrl());
    }

    @Blocking
    public void stopConsumer() throws InterruptedException {
        Log.info("Stopping consumer for " + this.receiveMessageRequest.queueUrl());
        this.isConsumerRunning = false;
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutDown() throws InterruptedException {
        this.stopConsumer();
    }

    private Long getDelay() {
        if (isBatchReady) {
            return waitTimeInMilliSeconds;
        } else {
            Long delayInMilliseconds = (3 * waitTimeInMilliSeconds) < (20 * 1000) ? (20 * 1000) : (3 * waitTimeInMilliSeconds);
            Log.info("Received Empty messages for " + this.receiveMessageRequest.queueUrl() + ". Waiting for " + delayInMilliseconds / 1000 + " secs before next poll.");
            return delayInMilliseconds;
        }
    }
}

