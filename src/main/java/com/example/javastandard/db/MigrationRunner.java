package com.example.javastandard.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class MigrationRunner {
    private static final List<String> MIGRATIONS =
            Arrays.asList("0001_auth.sql");
    private final DataSource dataSource;

    public MigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS java_standard_schema_migrations "
                        + "(filename TEXT PRIMARY KEY, applied_at INTEGER NOT NULL)");
            }
            for (String migration : MIGRATIONS) {
                if (!isApplied(connection, migration)) {
                    apply(connection, migration);
                }
            }
            connection.commit();
        } catch (Exception exception) {
            throw new IllegalStateException("PostgreSQL migration failed.", exception);
        }
    }

    private boolean isApplied(Connection connection, String filename) throws SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM java_standard_schema_migrations WHERE filename = ?")) {
            statement.setString(1, filename);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void apply(Connection connection, String filename) throws IOException, SQLException {
        ClassPathResource resource = new ClassPathResource("db/migration/" + filename);
        String sql;
        try (InputStream stream = resource.getInputStream()) {
            byte[] bytes = new byte[(int) resource.contentLength()];
            int offset = 0;
            int read;
            while (offset < bytes.length && (read = stream.read(bytes, offset, bytes.length - offset)) > 0) {
                offset += read;
            }
            sql = new String(bytes, StandardCharsets.UTF_8);
        }
        try (Statement statement = connection.createStatement()) {
            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO java_standard_schema_migrations(filename, applied_at) VALUES (?, ?)")) {
            statement.setString(1, filename);
            statement.setLong(2, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }
}
