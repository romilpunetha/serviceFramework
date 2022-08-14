package com.services.util.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;

import javax.validation.constraints.NotBlank;

public interface MetricsFactory {

    Counter getCounter(@NotBlank String name, Tags tags);

    void increment(String name, Tags tags);

}
