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
                // If we have raw JSON saved, start with that
                node = (ObjectNode) objectMapper.readTree(settings.getRawSettingsJson());
            } else {
                // Create a new node and populate with legacy fields
                node = objectMapper.createObjectNode();
            }

            // Always ensure critical fields from legacy DB are synced into the JSON
            // response
            node.put("merchantName", settings.getMerchantName() != null ? settings.getMerchantName() : "");
            node.put("taxRate", settings.getTaxRate());
            node.put("upiId", settings.getUpiId() != null ? settings.getUpiId() : "");
            node.put("shopAddress", settings.getAddress() != null ? settings.getAddress() : "");
            node.put("gstNumber", settings.getGstNumber() != null ? settings.getGstNumber() : "");
            node.put("isTaxInclusive", settings.isTaxInclusive());

            // Handle tableAreas string -> array/string conversion if needed
            if (settings.getTableAreas() != null) {
                node.put("tableAreas", settings.getTableAreas());
            }

            return ResponseEntity.ok(node);
        } catch (Exception e) {
            // Safe fallback if JSON parsing fails
            ObjectNode fallbackNode = objectMapper.createObjectNode();
            fallbackNode.put("merchantName", settings.getMerchantName() != null ? settings.getMerchantName() : "");
            fallbackNode.put("taxRate", settings.getTaxRate());
            fallbackNode.put("upiId", settings.getUpiId() != null ? settings.getUpiId() : "");
            fallbackNode.put("shopAddress", settings.getAddress() != null ? settings.getAddress() : "");
            fallbackNode.put("gstNumber", settings.getGstNumber() != null ? settings.getGstNumber() : "");
            fallbackNode.put("isTaxInclusive", settings.isTaxInclusive());
            if (settings.getTableAreas() != null) {
                fallbackNode.put("tableAreas", settings.getTableAreas());
            }
            return ResponseEntity.ok(fallbackNode);
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
            // 1. Fetch existing legacy settings
            Settings settings = commonDataService.getSettings(restaurantId);

            // 2. Sync incoming JSON fields to legacy columns for stability
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
            if (settingsNode.has("taxInclusive"))
                settings.setTaxInclusive(settingsNode.get("taxInclusive").asBoolean());

            if (settingsNode.has("taxMapping"))
                settings.setTaxMapping(settingsNode.get("taxMapping").toString());
            if (settingsNode.has("tableAreas"))
                settings.setTableAreas(settingsNode.get("tableAreas").asText());

            // 3. Store the WHOLE JSON blob for future proofing
            settings.setRawSettingsJson(objectMapper.writeValueAsString(settingsNode));

            // 4. Save to the stable repository
            settingsRepository.save(settings);

            return ResponseEntity.ok(Map.of("ok", true, "message", "Settings saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "message", "Update failed: " + e.getMessage()));
        }
    }
}
