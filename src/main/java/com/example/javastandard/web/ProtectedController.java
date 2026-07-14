package com.example.javastandard.web;

import com.example.javastandard.auth.AuthPrincipal;
import com.example.javastandard.auth.AuthException;
import com.example.javastandard.auth.AuthService;
import com.example.javastandard.auth.AuthUser;
import com.example.javastandard.db.model.UserRecord;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protected")
public class ProtectedController {
    private final AuthService authService;

    public ProtectedController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        UserRecord user = authService.findUserById(principal.getUserId());
        if (user == null || !user.isActive()) {
            throw new AuthException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "AUTH_UNAUTHORIZED", "Unauthorized");
        }
        AuthUser currentUser = new AuthUser(user);
        Map<String, Object> profile = new LinkedHashMap<String, Object>();
        profile.put("email", currentUser.getEmail());
        profile.put("role", currentUser.getRole());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("profile", profile);
        return result;
    }
}
