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

@RestController
@RequestMapping("/api/dine-in")
@CrossOrigin(origins = "*")
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
        if (tableOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Table not found"));
        }

        // 2. Validate Location
        Optional<Settings> settingsOpt = settingsRepository.findById("default");

        if (settingsOpt.isPresent()) {
            Settings settings = settingsOpt.get();

            // Check if Store Location is configured
            if (settings.getLatitude() != 0.0 || settings.getLongitude() != 0.0) {
                double distance = LocationUtils.distance(
                        request.getLatitude(), request.getLongitude(),
                        settings.getLatitude(), settings.getLongitude());

                // Strict 5 meters radius as per requirements
                if (distance > 5.0) {
                    return ResponseEntity.status(403).body(Map.of(
                            "ok", false,
                            "message", "You must be inside the restaurant to order.",
                            "distance_meters", distance));
                }
            } else {
                // Configuring location is mandatory for this feature
                return ResponseEntity.status(503)
                        .body(Map.of("ok", false, "message", "Restaurant location not configured."));
            }
        } else {
            return ResponseEntity.status(503).body(Map.of("ok", false, "message", "System config missing."));
        }

        // 3. Success
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Table verified",
                "table", tableOpt.get()));
    }
}
