package com.pos.server.controller;

import com.pos.server.model.Setting;
import com.pos.server.model.User;
import com.pos.server.repository.SettingRepository;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class SettingsController {

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.pos.server.service.CommonDataService commonDataService;

    private String getRestaurantId(String headerId) {
        if (headerId != null && !headerId.equalsIgnoreCase("null") && !headerId.isEmpty()) {
            return headerId.toLowerCase().trim();
        }
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null)
                return null;
            String username = auth.getName();
            User user = authService.findByUsername(username);
            return user != null ? user.getRestaurantId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping
    public ResponseEntity<?> getSettings(@RequestHeader(value = "X-Restaurant-Id", required = false) String headerId) {
        String restaurantId = getRestaurantId(headerId);
        if (restaurantId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized or Restaurant ID missing"));
        }

        try {
            // Attempt to fetch from new JSON-based settings table (pos_terminal_settings)
            // Wrapping in try-catch to handle SQLGrammarException if table doesn't exist
            // yet in production
            return settingRepository.findByRestaurantId(restaurantId)
                    .map(setting -> {
                        try {
                            String json = setting.getSettingsJson();
                            if (json == null || json.trim().isEmpty() || json.equalsIgnoreCase("null")) {
                                return (ResponseEntity<?>) ResponseEntity
                                        .ok(commonDataService.getSettings(restaurantId));
                            }

                            com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper
                                    .readTree(json);

                            // Sync legacy fields
                            com.pos.server.model.Settings legacy = commonDataService.getSettings(restaurantId);
                            if (legacy != null) {
                                if (!node.has("merchantName"))
                                    node.put("merchantName", legacy.getMerchantName());
                                if (!node.has("taxRate"))
                                    node.put("taxRate", legacy.getTaxRate());
                                if (!node.has("upiId"))
                                    node.put("upiId", legacy.getUpiId());
                                if (!node.has("shopAddress"))
                                    node.put("shopAddress", legacy.getAddress());
                            }
                            return (ResponseEntity<?>) ResponseEntity.ok(node);
                        } catch (Exception e) {
                            return (ResponseEntity<?>) ResponseEntity.ok(commonDataService.getSettings(restaurantId));
                        }
                    })
                    .orElseGet(
                            () -> (ResponseEntity<?>) ResponseEntity.ok(commonDataService.getSettings(restaurantId)));
        } catch (Exception e) {
            // Table might not exist yet (SQLGrammarException)
            System.err.println("[SettingsBackup] Table pos_terminal_settings likely missing, falling back to legacy: "
                    + e.getMessage());
            return ResponseEntity.ok(commonDataService.getSettings(restaurantId));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(
            @RequestBody com.fasterxml.jackson.databind.node.ObjectNode settingsNode,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String headerId) {

        String restaurantId = getRestaurantId(headerId);
        if (restaurantId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        try {
            // 1. Sync critical fields to legacy 'pos_settings' table
            com.pos.server.model.Settings legacy = commonDataService.getSettings(restaurantId);
            if (settingsNode.has("merchantName"))
                legacy.setMerchantName(settingsNode.get("merchantName").asText());
            if (settingsNode.has("taxRate"))
                legacy.setTaxRate(settingsNode.get("taxRate").asDouble());
            if (settingsNode.has("upiId"))
                legacy.setUpiId(settingsNode.get("upiId").asText());

            // Handle both names used in frontend
            if (settingsNode.has("shopAddress"))
                legacy.setAddress(settingsNode.get("shopAddress").asText());
            else if (settingsNode.has("address"))
                legacy.setAddress(settingsNode.get("address").asText());

            if (settingsNode.has("gstNumber"))
                legacy.setGstNumber(settingsNode.get("gstNumber").asText());

            if (settingsNode.has("taxMapping"))
                legacy.setTaxMapping(settingsNode.get("taxMapping").toString());

            if (settingsNode.has("isTaxInclusive"))
                legacy.setTaxInclusive(settingsNode.get("isTaxInclusive").asBoolean());

            commonDataService.updateSettings(legacy, restaurantId);

            // 2. Persist full state to new JSON-based table (Optional/Safe fallback)
            try {
                Setting setting = settingRepository.findByRestaurantId(restaurantId).orElse(new Setting());
                setting.setRestaurantId(restaurantId);
                setting.setSettingsJson(objectMapper.writeValueAsString(settingsNode));
                settingRepository.save(setting);
            } catch (Exception e) {
                System.err.println(
                        "[SettingsSafety] Could not save to JSON table, but legacy sync succeeded: " + e.getMessage());
            }

            return ResponseEntity.ok(Map.of("ok", true, "message", "Settings synchronized globally"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "message", "Update failed: " + e.getMessage()));
        }
    }
}
