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

import java.util.Map;
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

        if (settingsOpt.isPresent()) {
            Settings settings = settingsOpt.get();

            // Check if Store Location is configured
            if (settings.getLatitude() != null && settings.getLongitude() != null) {
                double distance = LocationUtils.distance(
                        request.getLatitude(), request.getLongitude(),
                        settings.getLatitude(), settings.getLongitude());

                int radius = settings.getGeofenceRadius() != null ? settings.getGeofenceRadius() : 50;

                if (distance > radius) {
                    return ResponseEntity.status(403).body(Map.of(
                            "ok", false,
                            "message", "You must be inside the restaurant to order.",
                            "distance_meters", distance));
                }
            } else {
                // If location isn't configured, we assume validation is bypassed for now to
                // allow testing
                log.warn("Restaurant {} location not configured. Skipping location check.", restaurantId);
            }
        }

        // 3. Success
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Table verified",
                "table", tableOpt.get()));
    }
}
