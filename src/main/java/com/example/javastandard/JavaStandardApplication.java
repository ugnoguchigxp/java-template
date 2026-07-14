package com.example.javastandard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.javastandard.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class JavaStandardApplication {
    public static void main(String[] args) {
        SpringApplication.run(JavaStandardApplication.class, args);
    }
}
