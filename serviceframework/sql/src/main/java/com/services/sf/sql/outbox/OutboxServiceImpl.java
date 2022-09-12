package com.services.sf.sql.outbox;

import com.services.common.domain.base.AvroData;
import com.services.common.domain.basesql.Outbox;
import com.services.common.domain.util.LocalContext;
import com.services.sf.commons.base.outbox.OutboxSerializer;
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
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ApplicationScoped
public class OutboxServiceImpl
        implements OutboxService {


    final OutboxRepository repository;
    @Inject
    protected LocalContext localContext;
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
 
    public Uni<Outbox> create(Outbox e) {
        return repository.create(e);
    }

    public Multi<String> bulkCreate(List<Outbox> eList) {
        return repository.bulkCreate(eList);
    }

    @Override
    public Uni<List<Outbox>> bulkCreateWithResponse(List<Outbox> eList) {
        return repository.bulkCreateWithResponse(eList);
    }

    public Outbox convertToOutbox(AvroData t, String aggregateType, String eventType, Map<String, Object> additionalFieldValues) {

        String topic = StringUtils.isAllBlank(t.getTopic()) ? appName + ".outbox.event." + aggregateType : t.getTopic();

        byte[] payload = serializer.serialize(topic, t.getPayload());

        return Outbox.builder()
                .additionalFieldValues(additionalFieldValues)
                .aggregateType(aggregateType)
                .eventType(eventType)
                .expireAt(Instant.now().plus(outboxPersistenceInDays, ChronoUnit.DAYS))
                .aggregateId(t.getDomain().getId())
                .payload(payload)
                .eventOccurredAt(t.getDomain().getLastModifiedAt())
                .build();
    }
}
