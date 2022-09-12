package com.services.sf.commons.base.outbox;

import io.quarkus.arc.properties.IfBuildProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.common.header.Headers;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
@IfBuildProperty(name = "framework.outbox.serializer", stringValue = "defaultSerializer")
public class DefaultOutboxSerializer implements OutboxSerializer {

    public DefaultOutboxSerializer() {
    }

    public byte[] serialize(String topic, Object record) {
        return null;
    }

    public byte[] serialize(String topic, Headers headers, Object record) {
        return null;
    }
}
