package com.services.sf.commons.base.outbox;

import org.apache.kafka.common.header.Headers;

public interface OutboxSerializer {
    byte[] serialize(String topic, Object record);

    byte[] serialize(String topic, Headers headers, Object record);
}
