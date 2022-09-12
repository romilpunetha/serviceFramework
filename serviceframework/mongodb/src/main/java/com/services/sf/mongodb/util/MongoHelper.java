package com.services.sf.mongodb.util;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.ClientSession;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor
@RequestScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.mongodb.enabled", stringValue = "true")
public class MongoHelper {

    @Inject
    protected ReactiveMongoClient reactiveMongoClient;

    @Getter
    @Setter
    protected ClientSession clientSessionPrimary = null;

    @Getter
    @Setter
    protected ClientSession clientSessionSecondary = null;

    @Getter
    @Setter
    protected ClientSession clientSessionOutbox = null;


    public Uni<ClientSession> createClientSessionPrimary() {

        Objects.requireNonNull(reactiveMongoClient);

        return this.createClientSessionPrimary(ClientSessionOptions.builder()
                .defaultTransactionOptions(TransactionOptions
                        .builder()
                        .maxCommitTime(10 * 1000L, TimeUnit.MILLISECONDS)
                        .readPreference(ReadPreference.primary())
                        .writeConcern(WriteConcern.MAJORITY)
                        .build())
                .build());
    }


    public Uni<ClientSession> createClientSessionPrimary(Long maxCommitTimeInMS) {

        Objects.requireNonNull(reactiveMongoClient);

        return this.createClientSessionPrimary(ClientSessionOptions.builder()
                .defaultTransactionOptions(TransactionOptions
                        .builder()
                        .maxCommitTime(maxCommitTimeInMS, TimeUnit.MILLISECONDS)
                        .readPreference(ReadPreference.primary())
                        .writeConcern(WriteConcern.MAJORITY)
                        .build())
                .build());
    }

    public Uni<ClientSession> createClientSessionSecondary() {

        Objects.requireNonNull(reactiveMongoClient);

        return this.createClientSessionSecondary(ClientSessionOptions.builder()
                .defaultTransactionOptions(TransactionOptions
                        .builder()
                        .maxCommitTime(10 * 1000L, TimeUnit.MILLISECONDS)
                        .readPreference(ReadPreference.secondaryPreferred())
                        .writeConcern(WriteConcern.MAJORITY)
                        .build())
                .build());
    }

    public Uni<ClientSession> createClientSessionSecondary(Long maxCommitTimeInMS) {

        Objects.requireNonNull(reactiveMongoClient);

        return this.createClientSessionSecondary(ClientSessionOptions.builder()
                .defaultTransactionOptions(TransactionOptions
                        .builder()
                        .maxCommitTime(maxCommitTimeInMS, TimeUnit.MILLISECONDS)
                        .readPreference(ReadPreference.secondaryPreferred())
                        .writeConcern(WriteConcern.MAJORITY)
                        .build())
                .build());
    }

    public Uni<ClientSession> createClientSessionPrimary(ClientSessionOptions clientSessionOptions) {

        Objects.requireNonNull(reactiveMongoClient);

        return reactiveMongoClient.startSession(
                clientSessionOptions
        ).invoke(this::setClientSessionPrimary);
    }

    public Uni<ClientSession> createClientSessionSecondary(ClientSessionOptions clientSessionOptions) {

        Objects.requireNonNull(reactiveMongoClient);

        return reactiveMongoClient.startSession(
                clientSessionOptions
        ).invoke(this::setClientSessionSecondary);
    }

    public Uni<ClientSession> createClientSessionOutbox() {

        if (getClientSessionOutbox() != null)
            return Uni.createFrom().item(getClientSessionOutbox());

        return this.createClientSessionOutbox(ClientSessionOptions.builder()
                .defaultTransactionOptions(TransactionOptions
                        .builder()
                        .maxCommitTime(10 * 1000L, TimeUnit.MILLISECONDS)
                        .readPreference(ReadPreference.primary())
                        .writeConcern(WriteConcern.MAJORITY)
                        .build())
                .build());
    }

    public Uni<ClientSession> createClientSessionOutbox(ClientSessionOptions clientSessionOptions) {

        Objects.requireNonNull(reactiveMongoClient);

        return reactiveMongoClient.startSession(
                clientSessionOptions
        ).invoke(this::setClientSessionOutbox);
    }


    @PreDestroy
    public void closeClientSession() {
        if (clientSessionPrimary != null) clientSessionPrimary.close();
        if (clientSessionSecondary != null) clientSessionSecondary.close();
        if (clientSessionOutbox != null) clientSessionOutbox.close();
    }

}
