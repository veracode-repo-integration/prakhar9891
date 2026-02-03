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
        return settingRepository.findByRestaurantId(restaurantId)
                .map(setting -> {
                    try {
                        com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper
                                .readTree(setting.getSettingsJson());

                        // Merge high-priority legacy fields for consistency
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
                        return (ResponseEntity<?>) ResponseEntity.internalServerError().body("Error parsing settings");
                    }
                })
                .orElseGet(() -> {
                    // Fallback to legacy settings model
                    return (ResponseEntity<?>) ResponseEntity.ok(commonDataService.getSettings(restaurantId));
                });
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

            commonDataService.updateSettings(legacy, restaurantId);

            // 2. Persist full state to new JSON-based table
            Setting setting = settingRepository.findByRestaurantId(restaurantId).orElse(new Setting());
            setting.setRestaurantId(restaurantId);
            setting.setSettingsJson(objectMapper.writeValueAsString(settingsNode));
            settingRepository.save(setting);

            return ResponseEntity.ok(Map.of("ok", true, "message", "Settings synchronized globally"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
