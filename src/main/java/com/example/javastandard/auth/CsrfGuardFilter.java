package com.example.javastandard.auth;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CsrfGuardFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (requiresProtection(request.getMethod())
                && request.getRequestURI().startsWith("/api/")) {
            String cookie = cookie(request, AuthCookieService.CSRF_TOKEN);
            String header = request.getHeader("X-XSRF-TOKEN");
            if (cookie == null || header == null || !constantTimeEquals(cookie, header)) {
                response.setStatus(403);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Forbidden\",\"code\":\"CSRF_FORBIDDEN\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresProtection(String method) {
        return !("GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)
                || "TRACE".equalsIgnoreCase(method));
    }

    private String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) return false;
        int result = 0;
        for (int i = 0; i < left.length(); i++) result |= left.charAt(i) ^ right.charAt(i);
        return result == 0;
    }
}
