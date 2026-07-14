package com.example.javastandard.auth;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerCookieAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokens;

    public BearerCookieAuthenticationFilter(TokenService tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = bearer(request.getHeader("Authorization"));
        if (token == null) {
            token = cookie(request, AuthCookieService.ACCESS_TOKEN);
        }
        if (token != null) {
            try {
                AuthPrincipal principal = tokens.parseAccessToken(token);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_" + principal.getRole().toUpperCase())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String bearer(String value) {
        if (value == null || !value.startsWith("Bearer ")) return null;
        return value.substring("Bearer ".length()).trim();
    }

    private String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
