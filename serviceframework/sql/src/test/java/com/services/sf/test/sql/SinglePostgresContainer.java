package com.services.sf.test.sql;

import org.testcontainers.containers.PostgreSQLContainer;

public class SinglePostgresContainer extends PostgreSQLContainer<SinglePostgresContainer> {

    private static final String IMAGE_VERSION = "postgres:latest";
    private static SinglePostgresContainer container;

    public SinglePostgresContainer() {
        super(IMAGE_VERSION);
    }

    public static SinglePostgresContainer getInstance() {
        if (getContainer() == null) {
            setContainer(new SinglePostgresContainer());
        }
        return getContainer();
    }

    public static SinglePostgresContainer getContainer() {
        return container;
    }

    public static void setContainer(SinglePostgresContainer container) {
        SinglePostgresContainer.container = container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("DB_URL", getContainer().getJdbcUrl().substring(5));
        System.setProperty("DB_USERNAME", getContainer().getUsername());
        System.setProperty("DB_PASSWORD", getContainer().getPassword());
    }


    @Override
    public void stop() {
        // No-op. Shutdown only on JVM shutdown
    }

}