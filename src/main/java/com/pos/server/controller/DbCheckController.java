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
            String sql = "SELECT tc.constraint_name, tc.table_name, kcu.column_name, " +
                    "ccu.table_name AS foreign_table_name, " +
                    "ccu.column_name AS foreign_column_name " +
                    "FROM information_schema.table_constraints AS tc " +
                    "JOIN information_schema.key_column_usage AS kcu " +
                    "  ON tc.constraint_name = kcu.constraint_name " +
                    "  AND tc.table_schema = kcu.table_schema " +
                    "JOIN information_schema.constraint_column_usage AS ccu " +
                    "  ON ccu.constraint_name = tc.constraint_name " +
                    "  AND ccu.table_schema = tc.table_schema " +
                    "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name='pos_orders'";

            var results = jdbcTemplate.queryForList(sql);
            return Map.of("status", "success", "constraints", results);
        } catch (Exception e) {
            return Map.of("status", "error", "error", e.getMessage());
        }
    }

    @GetMapping("/fix-db")
    public Map<String, Object> fixDb() {
        try {
            // Drop the legacy constraint referencing 'customers' table
            String sql = "ALTER TABLE pos_orders DROP CONSTRAINT fk9ia55q1unwb6dqhw6stia7q0d";
            jdbcTemplate.execute(sql);
            return Map.of("status", "success", "message", "Constraint dropped successfully");
        } catch (Exception e) {
            return Map.of("status", "error", "error", e.getMessage());
        }
    }
}
