package com.example.javastandard.cli;

import com.example.javastandard.JavaStandardApplication;
import com.example.javastandard.db.MigrationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class DatabaseMigrateCommand {
    private DatabaseMigrateCommand() {}

   public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "cli");
       ConfigurableApplicationContext context = new SpringApplicationBuilder(JavaStandardApplication.class)
                .web(WebApplicationType.NONE).properties("spring.profiles.active=cli")
                .run(args);
        try {
            context.getBean(MigrationRunner.class).migrate();
        } finally {
            context.close();
        }
    }
}
