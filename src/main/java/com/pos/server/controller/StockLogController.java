package com.pos.server.controller;

import com.pos.server.model.StockLog;
import com.pos.server.repository.StockLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Stock Log management
 * Tracks all inventory movements and changes
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StockLogController {

    @Autowired
    private StockLogRepository stockLogRepository;

    @Autowired
    private com.pos.server.service.AuthService authService;

    /**
     * Get all stock logs
     * GET /api/stock-logs
     */
    @GetMapping("/stock-logs")
    public ResponseEntity<List<StockLog>> getAllStockLogs(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(stockLogRepository.findAllByRestaurantId(rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }

    /**
     * Get stock logs by product ID
     * GET /api/stock-logs/product/{productId}
     */
    @GetMapping("/stock-logs/product/{productId}")
    public ResponseEntity<List<StockLog>> getStockLogsByProduct(@PathVariable String productId,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(stockLogRepository.findByProductIdAndRestaurantId(productId, rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }

    /**
     * Get stock logs by type
    @GetMapping("/stock-logs/type/{type}")
    public ResponseEntity<List<StockLog>> getStockLogsByType(
            @PathVariable String type,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(stockLogRepository.findByTypeAndRestaurantId(type, rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }


    /**
     * Create stock log
     * POST /api/stock-logs
     */
    @PostMapping("/stock-logs")
    public ResponseEntity<?> createStockLog(@RequestBody StockLog stockLog,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            if (stockLog.getId() == null || stockLog.getId().isEmpty()) {
                stockLog.setId(java.util.UUID.randomUUID().toString());
            }
            if (stockLog.getCreatedAt() == null) {
                stockLog.setCreatedAt(Instant.now().toString());
            }
            stockLog.setRestaurantId(rid);

            StockLog saved = stockLogRepository.save(stockLog);

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("stockLog", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Delete stock log
     * DELETE /api/stock-logs/{id}
     */
    @DeleteMapping("/stock-logs/{id}")
    public ResponseEntity<?> deleteStockLog(@PathVariable String id,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            if (!stockLogRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Stock log not found"));
            }

            // Optional: Ensure it belongs to the RID
            StockLog log = stockLogRepository.findById(id).orElse(null);
            if (log != null && !rid.equals(log.getRestaurantId())) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Access denied"));
            }

            stockLogRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Stock log deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
