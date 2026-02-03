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

    private String getCurrentRestaurantId() {
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
    public ResponseEntity<?> getSettings() {
        String restaurantId = getCurrentRestaurantId();
        if (restaurantId == null) {
            return ResponseEntity.status(401).body("Unauthorized or Restaurant ID missing");
        }

        return settingRepository.findByRestaurantId(restaurantId)
                .map(setting -> {
                    try {
                        return ResponseEntity.ok(objectMapper.readTree(setting.getSettingsJson()));
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().body("Error parsing settings");
                    }
                })
                .orElse(ResponseEntity.ok(objectMapper.createObjectNode()));
    }

    @PutMapping
    public ResponseEntity<?> updateSettings(@RequestBody JsonNode settingsNode) {
        String restaurantId = getCurrentRestaurantId();
        if (restaurantId == null) {
            return ResponseEntity.status(401).body("Unauthorized or Restaurant ID missing");
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
