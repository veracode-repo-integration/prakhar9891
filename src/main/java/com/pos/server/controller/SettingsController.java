package com.pos.server.controller;

import com.pos.server.model.Setting;
import com.pos.server.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingRepository settingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<?> getSettings(
            @RequestAttribute(value = "restaurantId", required = false) String restaurantId) {
        if (restaurantId == null) {
            return ResponseEntity.badRequest().body("Restaurant ID required");
        }

        return settingRepository.findByRestaurantId(restaurantId)
                .map(setting -> {
                    try {
                        return ResponseEntity.ok(objectMapper.readTree(setting.getSettingsJson()));
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().body("Error parsing settings");
                    }
                })
                .orElse(ResponseEntity.ok(objectMapper.createObjectNode())); // Return empty object if no settings found
    }

    @PostMapping
    public ResponseEntity<?> updateSettings(
            @RequestAttribute(value = "restaurantId", required = false) String restaurantId,
            @RequestBody JsonNode settingsNode) {

        if (restaurantId == null) {
            return ResponseEntity.badRequest().body("Restaurant ID required");
        }

        try {
            String settingsJson = objectMapper.writeValueAsString(settingsNode);

            Setting setting = settingRepository.findByRestaurantId(restaurantId)
                    .orElse(new Setting());

            setting.setRestaurantId(restaurantId);
            setting.setSettingsJson(settingsJson);

            settingRepository.save(setting);

            return ResponseEntity.ok("Settings saved successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error saving settings: " + e.getMessage());
        }
    }
}
