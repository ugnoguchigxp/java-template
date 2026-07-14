package com.example.javastandard;

import com.example.javastandard.auth.AuthService;
import com.example.javastandard.web.LoginRequest;
import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "DATABASE_URL=build/test-data/application.sqlite",
        "JWT_SECRET=test-secret-that-is-at-least-32-characters-long",
        "APP_URL=http://localhost:8080",
        "CORS_ORIGINS=http://localhost:8080"
})
public class ApplicationSmokeTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthService authService;
    @Autowired
    private DataSource dataSource;

    private String email;

    @BeforeEach
    void createAdmin() {
        email = "admin-" + System.nanoTime() + "@example.com";
        authService.createAdmin(email, "Admin User", "password123456");
    }

    @Test
    void migratesAndServesHealthAndStaticRoutes() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/showcase"))
                .andExpect(status().isOk());
        try (Connection connection = dataSource.getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM java_standard_schema_migrations")) {
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (ResultSet result = connection.createStatement().executeQuery("PRAGMA foreign_keys")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
            }
            try (ResultSet result = connection.createStatement().executeQuery("PRAGMA journal_mode")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualToIgnoringCase("wal");
            }
            try (ResultSet result = connection.createStatement().executeQuery("PRAGMA busy_timeout")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(5000);
            }
        }
    }

    @Test
    void loginMeProtectedAndLogoutUseCookiesAndCsrf() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andReturn();
        jakarta.servlet.http.Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        String token = csrfCookie.getValue();
        String json = "{\"email\":\"" + email + "\",\"password\":\"password123456\"}";

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie access = login.getResponse().getCookie("access_token");
        jakarta.servlet.http.Cookie refresh = login.getResponse().getCookie("refresh_token");
        assertThat(access).isNotNull();
        assertThat(refresh).isNotNull();

        mockMvc.perform(get("/api/auth/me").cookie(access))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/protected/profile").cookie(access))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/protected/profile")
                        .header("Authorization", "Bearer " + access.getValue()))
                .andExpect(status().isOk());

        assertThat(login.getResponse().getHeaders("Set-Cookie").toString())
                .contains("HttpOnly", "SameSite=Lax");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(csrfCookie, access, refresh)
                        .header("X-XSRF-TOKEN", token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void mutatingRequestWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@example.com\",\"password\":\"password123456\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/protected/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownApiRoutesAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenRotatesAndRejectsReplay() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andReturn();
        jakarta.servlet.http.Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        String token = csrfCookie.getValue();
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie oldRefresh = login.getResponse().getCookie("refresh_token");
        assertThat(oldRefresh).isNotNull();

        MvcResult rotated = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(csrfCookie, oldRefresh)
                        .header("X-XSRF-TOKEN", token))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie newRefresh = rotated.getResponse().getCookie("refresh_token");
        assertThat(newRefresh).isNotNull();
        assertThat(newRefresh.getValue()).isNotEqualTo(oldRefresh.getValue());

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(csrfCookie, oldRefresh)
                        .header("X-XSRF-TOKEN", token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(csrfCookie, newRefresh)
                        .header("X-XSRF-TOKEN", token))
                .andExpect(status().isOk());
    }

    @Test
    void invalidLoginPayloadReturnsBadRequest() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andReturn();
        jakarta.servlet.http.Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andReturn();
        jakarta.servlet.http.Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveUserIsRejectedByCurrentUserAndProtectedEndpoints() throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/csrf")).andReturn();
        jakarta.servlet.http.Cookie csrfCookie = csrf.getResponse().getCookie("XSRF-TOKEN");
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123456\"}"))
                .andExpect(status().isOk())
                .andReturn();
        jakarta.servlet.http.Cookie access = login.getResponse().getCookie("access_token");
        try (Connection connection = dataSource.getConnection();
             java.sql.PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET is_active = 0 WHERE normalized_email = ?")) {
            statement.setString(1, email);
            statement.executeUpdate();
        }

        mockMvc.perform(get("/api/auth/me").cookie(access))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/protected/profile").cookie(access))
                .andExpect(status().isUnauthorized());
    }
}
