package com.services.sf.domain.basemongo.outbox;

import com.mongodb.reactivestreams.client.ClientSession;
import com.services.common.domain.base.AvroData;
import com.services.common.domain.basemongo.Outbox;
import com.services.sf.domain.base.outbox.OutboxSerializer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class OutboxServiceImpl
        implements OutboxService {

    final OutboxRepository repository;

    @Inject
    OutboxSerializer serializer;

    @ConfigProperty(name = "k8s.app.name", defaultValue = "default")
    String appName;
    @ConfigProperty(name = "framework.outbox.persistence.days", defaultValue = "365")
    Integer outboxPersistenceInDays;


    @Inject
    protected OutboxServiceImpl(OutboxRepository repository) {
        this.repository = repository;
    }

    public Uni<Outbox> create(Outbox e, ClientSession clientSession) {
        return repository.create(e, clientSession);
    }

    public Multi<String> bulkCreate(List<Outbox> eList, ClientSession clientSession) {
        return repository.bulkCreate(eList, clientSession);
    }

    public Outbox convertToOutbox(AvroData avroData, String aggregateType, String eventType, Map<String, Object> additionalFieldValues) {
        return this.convertToOutbox(List.of(avroData), aggregateType, eventType, additionalFieldValues).get(0);
    }

    public List<Outbox> convertToOutbox(List<AvroData> avroDataList, String aggregateType, String eventType, Map<String, Object> additionalFieldValues) {
        List<Outbox> outboxes = new ArrayList<>();

        for (AvroData t : avroDataList) {

            String topic = StringUtils.isAllBlank(t.getTopic()) ? appName + ".outbox.event." + aggregateType : t.getTopic();

            byte[] payload = serializer.serialize(topic, t.getPayload());

            outboxes.add(Outbox.builder()
                    .additionalFieldValues(additionalFieldValues)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .expireAt(Instant.now().plus(outboxPersistenceInDays, ChronoUnit.DAYS))
                    .aggregateId(t.getDomain().getId())
                    .payload(payload)
                    .eventOccurredAt(t.getDomain().getLastModifiedAt())
                    .build());
        }
        return outboxes;
    }


}

