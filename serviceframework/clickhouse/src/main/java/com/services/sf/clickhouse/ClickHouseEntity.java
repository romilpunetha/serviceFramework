package com.services.sf.clickhouse;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClickHouseEntity {
    String name() default "";
}
