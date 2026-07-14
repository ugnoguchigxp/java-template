package com.example.javastandard;

import com.example.javastandard.config.AppProperties;
import com.example.javastandard.config.EnvironmentValidator;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnvironmentValidatorTest {
    @Test
    void productionRejectsInsecureCookies() {
        AppProperties properties = properties();
        properties.setCookieSecure(false);
        Environment environment = productionEnvironment();

        assertThatThrownBy(() -> new EnvironmentValidator(properties, environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AUTH_COOKIE_SECURE=true is required in production.");
    }

    @Test
    void productionRejectsWildcardCors() {
        AppProperties properties = properties();
        properties.setCookieSecure(true);
        properties.setCorsOrigins("*");
        Environment environment = productionEnvironment();

        assertThatThrownBy(() -> new EnvironmentValidator(properties, environment).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Wildcard CORS is not allowed in production.");
    }

    private AppProperties properties() {
        AppProperties properties = new AppProperties();
        properties.setDatabasePath("build/test-data/validator.sqlite");
        properties.setJwtSecret("production-secret-that-is-at-least-32-characters");
        properties.setCorsOrigins("https://example.com");
        return properties;
    }

    private Environment productionEnvironment() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"production"});
        return environment;
    }
}
