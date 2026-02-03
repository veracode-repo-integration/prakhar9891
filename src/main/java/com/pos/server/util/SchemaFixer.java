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
        log.info("###############################################");
        log.info("###   DIAGNOSTIC: AUTO-SCHEMA REPAIR START  ###");
        log.info("###############################################");

        // 1. Check DB Connectivity
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("DB CONNECTIVITY TEST: SUCCESS (Result: {})", result);
        } catch (Exception e) {
            log.error("DB CONNECTIVITY TEST: FAILED - App will likely fail to serve requests. Error: {}",
                    e.getMessage());
        }

        // 2. Fix Foreign Key Constraints
        try {
            String[] constraints = {
                    "fk9ia55q1unwb6dqhw6stia7q0d",
                    "fk1itjildvo7y2dnfdnca25tjyv"
            };

            for (String constraint : constraints) {
                try {
                    jdbcTemplate.execute("ALTER TABLE pos_orders DROP CONSTRAINT IF EXISTS " + constraint);
                    log.info("Dropped/Checked legacy foreign key constraint: {}", constraint);
                } catch (Exception e) {
                    log.debug("Constraint {} drop failed (likely doesn't exist): {}", constraint, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("FK Repair failed: {}", e.getMessage());
        }

        // 3. Fix LOB to TEXT columns
        fixColumn("pos_orders", "note");
        fixColumn("pos_orders", "refunded_items_json");
        fixColumn("pos_products", "composition");
        fixColumn("pos_order_items", "modifiers_json");

        log.info("###############################################");
        log.info("###   DIAGNOSTIC: AUTO-SCHEMA REPAIR END    ###");
        log.info("###############################################");
    }

    private void fixColumn(String table, String column) {
        try {
            String checkSql = "SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ?";
            List<String> types = jdbcTemplate.query(checkSql, (rs, rowNum) -> rs.getString("data_type"), table, column);

            if (types.isEmpty()) {
                log.info("Column '{}.{}' not found (may not be created yet).", table, column);
                return;
            }

            String type = types.get(0);
            if ("text".equalsIgnoreCase(type))
                return;

            log.info("Converting column '{}.{}' from '{}' to TEXT...", table, column, type);
            // Using a safer ALTER TABLE that handles types better if possible
            jdbcTemplate.execute(
                    "ALTER TABLE " + table + " ALTER COLUMN " + column + " TYPE TEXT USING " + column + "::TEXT");
            log.info("Fixed column '{}.{}' to TEXT.", table, column);

        } catch (Exception e) {
            log.warn("Failed to fix column '{}.{}': {}", table, column, e.getMessage());
        }
    }
}
