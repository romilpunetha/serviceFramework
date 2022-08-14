package com.services.sf.test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.Map;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {


    SinglePostgresContainer db;
    boolean enabled = true;

    @Override
    public Map<String, String> start() {
        if (!enabled) {
            return Collections.emptyMap();
        }
        db = SinglePostgresContainer.getInstance();
        db.withInitScript("db/test-data.sql");
        db.start();
        return Collections.singletonMap("quarkus.datasource.url", SinglePostgresContainer.getContainer().getJdbcUrl());
    }

    @Override
    public void stop() {
        if (enabled) db.stop();
    }
}

