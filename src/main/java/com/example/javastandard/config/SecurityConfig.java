package com.example.javastandard.config;

import com.example.javastandard.auth.BearerCookieAuthenticationFilter;
import com.example.javastandard.auth.CsrfGuardFilter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey jwtSecretKey(AppProperties properties) {
        return new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(properties.getCorsOrigins().split("\\s*,\\s*"));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Profile("!cli")
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerCookieAuthenticationFilter bearerFilter,
            CsrfGuardFilter csrfFilter,
            AppProperties properties) throws Exception {
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Unauthorized\",\"code\":\"AUTH_UNAUTHORIZED\"}");
        };
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/index.html", "/favicon.ico", "/assets/**",
                        "/api/health", "/api/csrf",
                        "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/me", "/api/protected/**").authenticated()
                .anyRequest().permitAll())
                .headers(headers -> headers
                .contentSecurityPolicy(policy -> policy.policyDirectives(
                        "default-src 'self'; object-src 'none'; frame-ancestors 'self'; base-uri 'self';"))
                .frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(csrfFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
