package com.example.javastandard.cli;

import com.example.javastandard.JavaStandardApplication;
import com.example.javastandard.auth.AuthService;
import com.example.javastandard.db.MigrationRunner;
import java.io.Console;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class AuthCreateAdminCommand {
    private AuthCreateAdminCommand() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("spring.profiles.active", "cli");
        Map<String, String> options = parse(args);
        String email = required(options, "email");
        String name = required(options, "name");
        String password = options.get("password");
        if (password == null && options.containsKey("password-stdin")) {
            password = readLine();
        }
        if (password == null) password = System.getenv("ADMIN_PASSWORD");
        if (password == null) {
            Console console = System.console();
            if (console == null) {
                password = readLine();
            } else {
                char[] entered = console.readPassword("Password: ");
                if (entered == null) throw new IllegalArgumentException("Password is required.");
                password = new String(entered);
            }
        }
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Password must be at least 12 characters.");
        }
        ConfigurableApplicationContext context = new SpringApplicationBuilder(JavaStandardApplication.class)
                .web(WebApplicationType.NONE).properties("spring.profiles.active=cli").run(args);
        try {
            context.getBean(MigrationRunner.class).migrate();
            context.getBean(AuthService.class).createAdmin(email, name, password);
        } finally {
            context.close();
        }
        System.out.println("Admin created.");
    }

    private static String readLine() throws IOException {
        String value = new BufferedReader(new InputStreamReader(System.in, "UTF-8")).readLine();
        if (value == null) throw new IllegalArgumentException("Password is required.");
        return value;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("--" + name + " is required.");
        return value;
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) continue;
            String key = args[i].substring(2);
            if ("password-stdin".equals(key)) {
                result.put(key, "true");
            } else if (i + 1 < args.length) {
                result.put(key, args[++i]);
            }
        }
        return result;
    }
}
