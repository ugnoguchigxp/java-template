package com.example.javastandard.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/", "/login", "/showcase", "/protected"})
    public String index() {
        return "forward:/index.html";
    }
}
