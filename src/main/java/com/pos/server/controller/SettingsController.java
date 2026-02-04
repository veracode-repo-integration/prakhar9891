package com.pos.server.controller;

import com.pos.server.model.Settings;
import com.pos.server.model.User;
import com.pos.server.repository.SettingsRepository;
import com.pos.server.service.AuthService;
import com.pos.server.service.CommonDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class SettingsController {

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonDataService commonDataService;

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

        // Use CommonDataService to get the stable legacy settings
        Settings settings = commonDataService.getSettings(restaurantId);

        try {
            ObjectNode node;
            if (settings.getRawSettingsJson() != null && !settings.getRawSettingsJson().trim().isEmpty()) {
                node = (ObjectNode) objectMapper.readTree(settings.getRawSettingsJson());
            } else {
                node = objectMapper.createObjectNode();
            }

            // Merge legacy fields with null protection
            node.put("merchantName", settings.getMerchantName() != null ? settings.getMerchantName() : "");
            node.put("taxRate", settings.getTaxRate() != null ? settings.getTaxRate() : 0.0);
            node.put("upiId", settings.getUpiId() != null ? settings.getUpiId() : "");
            node.put("shopAddress", settings.getAddress() != null ? settings.getAddress() : "");
            node.put("gstNumber", settings.getGstNumber() != null ? settings.getGstNumber() : "");
            node.put("isTaxInclusive", settings.getTaxInclusive() != null ? settings.getTaxInclusive() : false);

            if (settings.getTableAreas() != null) {
                node.put("tableAreas", settings.getTableAreas());
            }

            return ResponseEntity.ok(node);
        } catch (Exception e) {
            // Ultimate fallback JSON
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("merchantName", settings.getMerchantName() != null ? settings.getMerchantName() : "");
            fallback.put("taxRate", settings.getTaxRate() != null ? settings.getTaxRate() : 0.0);
            fallback.put("isTaxInclusive", false);
            return ResponseEntity.ok(fallback);
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(
            @RequestBody ObjectNode settingsNode,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String headerId) {

        String restaurantId = getRestaurantId(headerId);
        if (restaurantId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        try {
            Settings settings = commonDataService.getSettings(restaurantId);

            if (settingsNode.has("merchantName"))
                settings.setMerchantName(settingsNode.get("merchantName").asText());
            if (settingsNode.has("taxRate"))
                settings.setTaxRate(settingsNode.get("taxRate").asDouble());
            if (settingsNode.has("upiId"))
                settings.setUpiId(settingsNode.get("upiId").asText());
            if (settingsNode.has("gstNumber"))
                settings.setGstNumber(settingsNode.get("gstNumber").asText());

            if (settingsNode.has("shopAddress"))
                settings.setAddress(settingsNode.get("shopAddress").asText());
            else if (settingsNode.has("address"))
                settings.setAddress(settingsNode.get("address").asText());

            if (settingsNode.has("isTaxInclusive"))
                settings.setTaxInclusive(settingsNode.get("isTaxInclusive").asBoolean());
            if (settingsNode.has("taxMapping"))
                settings.setTaxMapping(settingsNode.get("taxMapping").toString());
            if (settingsNode.has("tableAreas"))
                settings.setTableAreas(settingsNode.get("tableAreas").asText());

            settings.setRawSettingsJson(objectMapper.writeValueAsString(settingsNode));
            settingsRepository.save(settings);

            return ResponseEntity.ok(Map.of("ok", true, "message", "Settings saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "message", "Update failed: " + e.getMessage()));
        }
    }
}
