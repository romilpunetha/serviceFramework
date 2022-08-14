package com.services.util.messaging.configuration;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class QueueConfiguration {
    private static final Map<String, Boolean> queueStatus = new HashMap<>();

    private static boolean allQueueStatus = true;

    public static boolean isConsumerEnabled(String queueUrl) {
        boolean isConsumerEnabled = queueStatus.getOrDefault(queueUrl, true);
        return allQueueStatus && isConsumerEnabled;
    }

    public static void disableQueue(String queueUrl) {
        queueStatus.put(queueUrl, false);
    }

    public static void enableQueue(String queueUrl) {
        queueStatus.put(queueUrl, true);
    }

    public static void enableAll() {
        allQueueStatus = true;
    }

    public static void disableAll() {
        allQueueStatus = false;
    }

}
