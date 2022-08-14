package com.services.util.metrics;

import com.services.common.domain.util.LocalContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;

@ApplicationScoped
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetricsFactoryImpl implements MetricsFactory {

    @Getter
    protected MeterRegistry meterRegistry;

    @Inject
    protected LocalContext localContext;

    @Inject
    public MetricsFactoryImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Counter getCounter(@NotBlank String name, Tags tags) {
        tags = tags.and("tenantId", localContext.getTenantId());
        return meterRegistry.counter(name, tags);
    }

    public void increment(String name, Tags tags) {
        tags = tags.and("tenantId", localContext.getTenantId());
        meterRegistry.counter(name, tags).increment();
    }

}
