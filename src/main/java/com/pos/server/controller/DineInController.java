package com.pos.server.controller;

import com.pos.server.dto.DineInRequest;
import com.pos.server.model.RestaurantTable;
import com.pos.server.model.Settings;
import com.pos.server.repository.RestaurantTableRepository;
import com.pos.server.repository.SettingsRepository;
import com.pos.server.util.LocationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/dine-in")
@CrossOrigin(origins = "*")
@Slf4j
public class DineInController {

    @Autowired
    private RestaurantTableRepository tableRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Scan QR and Validate Location
     * POST /api/dine-in/scan
     */
    @PostMapping("/scan")
    public ResponseEntity<?> scanTable(@RequestBody DineInRequest request) {
        // 1. Validate Table
        if (request.getTableId() == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Table ID is required"));
        }

        Optional<RestaurantTable> tableOpt = tableRepository.findById(request.getTableId());

        // Fallback: Try Name Search if ID fails
        if (tableOpt.isEmpty() && request.getRestaurantId() != null) {
            // 1. Try exact name (e.g. "T1")
            tableOpt = tableRepository.findByNameAndRestaurantId(request.getTableId(), request.getRestaurantId());

            // 2. Try "Table " + name (e.g. User scanned "1", DB has "Table 1")
            if (tableOpt.isEmpty()) {
                tableOpt = tableRepository.findByNameAndRestaurantId("Table " + request.getTableId(),
                        request.getRestaurantId());
            }
        }

        if (tableOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Table not found"));
        }

        RestaurantTable table = tableOpt.get();
        String restaurantId = table.getRestaurantId();

        // 2. Validate Location (Use restaurant-specific settings)
        Optional<Settings> settingsOpt = (restaurantId == null || restaurantId.trim().isEmpty())
                ? settingsRepository.findById("default")
                : settingsRepository.findFirstByRestaurantId(restaurantId.toLowerCase().trim());

        Settings settings = settingsOpt.orElse(null);

        if (settings != null) {
            // Check if Store Location is configured
            if (settings.getLatitude() != null && settings.getLongitude() != null) {
                double distance = LocationUtils.distance(
                        request.getLatitude(), request.getLongitude(),
                        settings.getLatitude(), settings.getLongitude());

                int radius = settings.getGeofenceRadius() != null ? settings.getGeofenceRadius() : 50;

                if (distance > radius) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("ok", false);
                    errorResponse.put("message", "You must be inside the restaurant to order.");
                    errorResponse.put("distance_meters", distance);
                    errorResponse.put("merchantName", settings.getMerchantName() != null ? settings.getMerchantName() : "");
                    return ResponseEntity.status(403).body(errorResponse);
                }
            } else {
                // If location isn't configured, we assume validation is bypassed for now to allow testing
                log.warn("Restaurant {} location not configured. Skipping location check.", restaurantId);
            }
        }

        // 3. Prepare Response with Settings
        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("message", "Table verified");
        response.put("table", table);

        if (settings != null) {
            try {
                ObjectNode node;
                if (settings.getRawSettingsJson() != null && !settings.getRawSettingsJson().trim().isEmpty()) {
                    node = (ObjectNode) objectMapper.readTree(settings.getRawSettingsJson());
                } else {
                    node = objectMapper.createObjectNode();
                }

                // Merge legacy fields with null protection (same logic as SettingsController)
                node.put("merchantName", settings.getMerchantName() != null ? settings.getMerchantName() : "");
                node.put("taxRate", settings.getTaxRate() != null ? settings.getTaxRate() : 0.0);
                node.put("upiId", settings.getUpiId() != null ? settings.getUpiId() : "");
                node.put("shopAddress", settings.getAddress() != null ? settings.getAddress() : "");
                node.put("gstNumber", settings.getGstNumber() != null ? settings.getGstNumber() : "");
                node.put("isTaxInclusive", settings.getTaxInclusive() != null ? settings.getTaxInclusive() : false);
                node.put("stateCode", settings.getStateCode() != null ? settings.getStateCode() : "27");
                node.put("stateName", settings.getStateName() != null ? settings.getStateName() : "Maharashtra");
                node.put("gstCategory", settings.getGstCategory() != null ? settings.getGstCategory() : "Regular");
                node.put("invoicePrefix", settings.getInvoicePrefix() != null ? settings.getInvoicePrefix() : "INV-2026-");

                if (settings.getLatitude() != null) node.put("latitude", settings.getLatitude());
                if (settings.getLongitude() != null) node.put("longitude", settings.getLongitude());
                if (settings.getGeofenceRadius() != null) node.put("geofenceRadius", settings.getGeofenceRadius());
                node.put("disableStockDecrement", settings.getDisableStockDecrement() != null ? settings.getDisableStockDecrement() : false);

                response.put("settings", node);
            } catch (Exception e) {
                log.error("Failed to parse settings for scan response", e);
            }
        }

        return ResponseEntity.ok(response);
    }
}
