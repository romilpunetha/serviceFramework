package com.services.sf.commons.base;

import java.time.Instant;

public interface BaseAvroMapper<T, E> extends BaseMapper<T, E> {

    default Long toLong(Instant instant) {
        return instant.toEpochMilli();
    }

    default Instant toInstant(Long l) {
        return Instant.ofEpochMilli(l);
    }
}
