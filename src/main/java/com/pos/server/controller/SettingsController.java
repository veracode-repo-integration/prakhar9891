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
public class SettingsController {

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonDataService commonDataService;


    @GetMapping
    public ResponseEntity<?> getSettings(@RequestHeader(value = "X-Restaurant-Id", required = false) String headerId) {
        try {
            String restaurantId = authService.validateRestaurantAccess(headerId);

            // Use CommonDataService to get the stable legacy settings
            Settings settings = commonDataService.getSettings(restaurantId);

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

            node.put("stateCode", settings.getStateCode() != null ? settings.getStateCode() : "27");
            node.put("stateName", settings.getStateName() != null ? settings.getStateName() : "Maharashtra");
            node.put("gstCategory", settings.getGstCategory() != null ? settings.getGstCategory() : "Regular");
            node.put("invoicePrefix", settings.getInvoicePrefix() != null ? settings.getInvoicePrefix() : "INV-2025-");

            if (settings.getLatitude() != null)
                node.put("latitude", settings.getLatitude());
            if (settings.getLongitude() != null)
                node.put("longitude", settings.getLongitude());
            if (settings.getGeofenceRadius() != null)
                node.put("geofenceRadius", settings.getGeofenceRadius());

            node.put("disableStockDecrement",
                    settings.getDisableStockDecrement() != null ? settings.getDisableStockDecrement() : false);

            if (settings.getTableAreas() != null) {
                node.put("tableAreas", settings.getTableAreas());
            }

            return ResponseEntity.ok(node);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(
            @RequestBody ObjectNode settingsNode,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String headerId) {

        try {
            String restaurantId = authService.validateRestaurantAccess(headerId);
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

            if (settingsNode.has("stateCode"))
                settings.setStateCode(settingsNode.get("stateCode").asText());
            if (settingsNode.has("stateName"))
                settings.setStateName(settingsNode.get("stateName").asText());
            if (settingsNode.has("gstCategory"))
                settings.setGstCategory(settingsNode.get("gstCategory").asText());
            if (settingsNode.has("invoicePrefix"))
                settings.setInvoicePrefix(settingsNode.get("invoicePrefix").asText());

            if (settingsNode.has("latitude"))
                settings.setLatitude(settingsNode.get("latitude").asDouble());
            if (settingsNode.has("longitude"))
                settings.setLongitude(settingsNode.get("longitude").asDouble());
            if (settingsNode.has("geofenceRadius"))
                settings.setGeofenceRadius(settingsNode.get("geofenceRadius").asInt());

            if (settingsNode.has("disableStockDecrement"))
                settings.setDisableStockDecrement(settingsNode.get("disableStockDecrement").asBoolean());

            settings.setRawSettingsJson(objectMapper.writeValueAsString(settingsNode));
            settingsRepository.save(settings);

            return ResponseEntity.ok(Map.of("ok", true, "message", "Settings saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
