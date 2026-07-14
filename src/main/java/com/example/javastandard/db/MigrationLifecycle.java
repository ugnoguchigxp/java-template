package com.example.javastandard.db;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!cli")
public class MigrationLifecycle {
    private final MigrationRunner runner;

    public MigrationLifecycle(MigrationRunner runner) {
        this.runner = runner;
    }

    @PostConstruct
    public void migrateBeforeServing() {
        runner.migrate();
    }
}
