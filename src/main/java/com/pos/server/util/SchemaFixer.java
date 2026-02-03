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

        // 1. Fix Foreign Key Constraint
        try {
            String sql = "ALTER TABLE pos_orders DROP CONSTRAINT fk9ia55q1unwb6dqhw6stia7q0d";
            jdbcTemplate.execute(sql);
            log.info("Dropped legacy foreign key constraint.");
        } catch (Exception e) {
            // Constraint likely already gone
        }

        // 2. Fix LOB to TEXT columns
        fixColumn("note");
        fixColumn("refunded_items_json");

        log.info("--- AUTO-SCHEMA REPAIR COMPLETE ---");
    }

    private void fixColumn(String column) {
        try {
            // Check current type
            String checkSql = "SELECT data_type FROM information_schema.columns WHERE table_name = 'pos_orders' AND column_name = ?";
            List<String> types = jdbcTemplate.query(checkSql, (rs, rowNum) -> rs.getString("data_type"), column);

            if (types.isEmpty()) {
                log.warn("Column '{}' not found in pos_orders. It might be created by Hibernate later.", column);
                return;
            }

            String type = types.get(0);
            log.info("Column '{}' detected as type '{}'", column, type);

            if ("text".equalsIgnoreCase(type) || "character varying".equalsIgnoreCase(type)) {
                log.info("Column '{}' is already correct.", column);
                return;
            }

            log.info("Converting column '{}' from '{}' to TEXT...", column, type);

            // DROP and ADD (Data in these fields will be lost, but necessary for app
            // survival)
            jdbcTemplate.execute("ALTER TABLE pos_orders DROP COLUMN " + column);
            jdbcTemplate.execute("ALTER TABLE pos_orders ADD COLUMN " + column + " TEXT");
            log.info("Fixed column '{}' to TEXT.", column);

        } catch (Exception e) {
            log.warn("Failed to fix column '{}': {}", column, e.getMessage());
        }
    }
}
