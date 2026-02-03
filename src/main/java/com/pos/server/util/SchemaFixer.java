package com.pos.server.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Component
@Slf4j
public class SchemaFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("--- STARTING AUTO-SCHEMA REPAIR ---");

        // 1. Fix Foreign Key Constraints
        try {
            // Drop potential conflicting legacy constraints
            String[] constraints = {
                    "fk9ia55q1unwb6dqhw6stia7q0d", // Old constraint
                    "fk1itjildvo7y2dnfdnca25tjyv" // New conflicting constraint
            };

            for (String constraint : constraints) {
                try {
                    jdbcTemplate.execute("ALTER TABLE pos_orders DROP CONSTRAINT " + constraint);
                    log.info("Dropped legacy foreign key constraint: " + constraint);
                } catch (Exception e) {
                    // Ignore if not exists
                }
            }
        } catch (Exception e) {
            // Global safety
        }

        // 2. Fix LOB to TEXT columns
        fixColumn("pos_orders", "note");
        fixColumn("pos_orders", "refunded_items_json");
        fixColumn("pos_products", "composition");
        fixColumn("pos_order_items", "modifiers_json");

        log.info("--- AUTO-SCHEMA REPAIR COMPLETE ---");
    }

    private void fixColumn(String table, String column) {
        try {
            // Check current type
            String checkSql = "SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
            List<String> types = jdbcTemplate.query(checkSql, (rs, rowNum) -> rs.getString("data_type"), table, column);

            if (types.isEmpty()) {
                log.warn("Column '{}' not found in {}. It might be created by Hibernate later.", column, table);
                return;
            }

            String type = types.get(0);
            log.info("Column '{}.{}' detected as type '{}'", table, column, type);

            if ("text".equalsIgnoreCase(type) || "character varying".equalsIgnoreCase(type)) {
                log.info("Column '{}.{}' is already correct.", table, column);
                return;
            }

            log.info("Converting column '{}.{}' from '{}' to TEXT...", table, column, type);

            // DROP and ADD (Data in these fields will be lost, but necessary for app
            // survival)
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " TEXT");
            log.info("Fixed column '{}.{}' to TEXT.", table, column);

        } catch (Exception e) {
            log.warn("Failed to fix column '{}.{}': {}", table, column, e.getMessage());
        }
    }
}
