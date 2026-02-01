package com.pos.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DbCheckController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/db")
    public Map<String, Object> checkDb() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of("status", "success", "result", result, "message", "Database is connected!");
        } catch (Exception e) {
            return Map.of("status", "error", "error", e.getMessage());
        }
    }
}
