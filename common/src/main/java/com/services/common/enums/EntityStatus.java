package com.services.common.enums;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum EntityStatus {

    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    BLOCKED("BLOCKED"),
    SUSPENDED("SUSPENDED"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    ERROR("ERROR"),
    INITIATED("INITIATED"),
    SUBMITTED("SUBMITTED"),
    PROCESSING("PROCESSING"),
    IN_QUEUE("IN_QUEUE"),
    APPROVED("APPROVED"),
    VERIFIED("VERIFIED"),
    REJECTED("REJECTED"),
    ENDED("ENDED"),
    CANCELLED("CANCELLED"),
    DELETED("DELETED");

    @Getter
    private final String value;

    EntityStatus(String value) {
        this.value = value;
    }

    private static final Map<String, EntityStatus> ENTITY_STATUS_MAP;

    static {
        Map<String, EntityStatus> map = new ConcurrentHashMap<>();
        for (EntityStatus instance : EntityStatus.values()) {
            map.put(instance.getValue(), instance);
        }
        ENTITY_STATUS_MAP = Collections.unmodifiableMap(map);
    }

    public static EntityStatus get(String value) {
        return ENTITY_STATUS_MAP.get(value);
    }
}
