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

    /**
     * Get all stock logs
     * GET /api/stock-logs
     */
    @GetMapping("/stock-logs")
    public ResponseEntity<List<StockLog>> getAllStockLogs(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        if (restaurantId != null && !restaurantId.isEmpty()) {
            return ResponseEntity.ok(stockLogRepository.findAllByRestaurantId(restaurantId));
        }
        return ResponseEntity.ok(stockLogRepository.findAll());
    }

    /**
     * Get stock logs by product ID
     * GET /api/stock-logs/product/{productId}
     */
    @GetMapping("/stock-logs/product/{productId}")
    public ResponseEntity<List<StockLog>> getStockLogsByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(stockLogRepository.findByProductId(productId));
    }

    /**
     * Get stock logs by type
     * GET /api/stock-logs/type/{type}
     */
    @GetMapping("/stock-logs/type/{type}")
    public ResponseEntity<List<StockLog>> getStockLogsByType(@PathVariable String type) {
        return ResponseEntity.ok(stockLogRepository.findByType(type));
    }

    /**
     * Create stock log
     * POST /api/stock-logs
     */
    @PostMapping("/stock-logs")
    public ResponseEntity<?> createStockLog(@RequestBody StockLog stockLog,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            if (stockLog.getId() == null || stockLog.getId().isEmpty()) {
                stockLog.setId(java.util.UUID.randomUUID().toString());
            }
            if (stockLog.getCreatedAt() == null) {
                stockLog.setCreatedAt(Instant.now().toString());
            }
            if (restaurantId != null) {
                stockLog.setRestaurantId(restaurantId);
            }

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
    public ResponseEntity<?> deleteStockLog(@PathVariable String id) {
        try {
            if (!stockLogRepository.existsById(id)) {
                Map<String, Object> error = new HashMap<>();
                error.put("ok", false);
                error.put("message", "Stock log not found");
                return ResponseEntity.status(404).body(error);
            }

            stockLogRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("message", "Stock log deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
