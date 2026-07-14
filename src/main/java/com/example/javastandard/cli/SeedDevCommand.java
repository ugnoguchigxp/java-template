package com.example.javastandard.cli;

import com.example.javastandard.JavaStandardApplication;
import com.example.javastandard.auth.AuthException;
import com.example.javastandard.auth.AuthService;
import com.example.javastandard.db.MigrationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class SeedDevCommand {
    private SeedDevCommand() {}

   public static void main(String[] args) {
        if (isProductionRequested()) {
            throw new IllegalStateException("seed:dev is not allowed in production.");
        }
        System.setProperty("spring.profiles.active", "cli");
        ConfigurableApplicationContext context = new SpringApplicationBuilder(JavaStandardApplication.class)
                .web(WebApplicationType.NONE).properties("spring.profiles.active=cli").run(args);
        try {
            context.getBean(MigrationRunner.class).migrate();
            AuthService service = context.getBean(AuthService.class);
            if (service.findUserByEmail("admin@example.com") == null) {
                try {
                    service.createAdmin("admin@example.com", "Admin User", "password123456");
                } catch (AuthException exception) {
                    if (!"AUTH_EMAIL_EXISTS".equals(exception.getCode())) throw exception;
                }
            }
        } finally {
            context.close();
        }
        System.out.println("Development seed complete.");
    }

    private static boolean isProductionRequested() {
        String value = System.getenv("SPRING_PROFILES_ACTIVE");
        if (value == null) return false;
        for (String profile : value.split(",")) {
            if ("production".equalsIgnoreCase(profile.trim())) return true;
        }
        return false;
    }
}
