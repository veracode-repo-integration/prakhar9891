package com.pos.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> welcome() {
        return Map.of(
                "status", "online",
                "message", "Welcome to OJAS POS Backend!",
                "api_version", "1.0.0",
                "documentation", "/swagger-ui.html");
    }
}
