package com.example.javastandard.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    public Map<String, String> health() {
        Map<String, String> response = new LinkedHashMap<String, String>();
        response.put("status", "ok");
        response.put("service", "java25-postgresql-template");
        return response;
    }
}
