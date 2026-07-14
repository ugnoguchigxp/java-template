package com.example.javastandard.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {
    @Bean
    public DataSource dataSource(AppProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        String url = "jdbc:sqlite:" + properties.getDatabasePath()
                + "?foreign_keys=true&journal_mode=WAL&busy_timeout=5000";
        dataSource.setJdbcUrl(url);
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setMaximumPoolSize(4);
        dataSource.setConnectionTimeout(5000L);
        return dataSource;
    }
}
