package com.pos.server.controller;

import com.pos.server.repository.ProductRepository;
import com.pos.server.repository.RestaurantRepository;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private AuthService authService;

    @GetMapping("/status")
    public ResponseEntity<?> getSyncStatus(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            
            // 1. Get latest product update
            LocalDateTime latestProductUpdate = productRepository.findLatestUpdateByRestaurantId(rid);
            long productTs = latestProductUpdate != null ? latestProductUpdate.toEpochSecond(ZoneOffset.UTC) : 0;

            // 2. Get restaurant (settings) update
            long restaurantTs = restaurantRepository.findById(rid)
                    .map(r -> r.getUpdatedAt() != null ? r.getUpdatedAt() : 0L)
                    .orElse(0L);

            Map<String, Object> status = new HashMap<>();
            status.put("ok", true);
            status.put("productTimestamp", productTs);
            status.put("settingsTimestamp", restaurantTs);
            status.put("serverTime", System.currentTimeMillis() / 1000);

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Access denied"));
        }
    }
}
