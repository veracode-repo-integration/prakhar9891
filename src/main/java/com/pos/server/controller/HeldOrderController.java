package com.pos.server.controller;

import com.pos.server.model.HeldOrder;
import com.pos.server.repository.HeldOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Held Order (Parked Order) management
 * Allows temporarily parking orders before checkout
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HeldOrderController {

    @Autowired
    private HeldOrderRepository heldOrderRepository;

    @Autowired
    private com.pos.server.service.AuthService authService;

    /**
     * Get all held orders
     * GET /api/held-orders
     */
    @GetMapping("/held-orders")
    public ResponseEntity<List<HeldOrder>> getAllHeldOrders(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(heldOrderRepository.findAllByRestaurantId(rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }

    /**
     * Get held order by ID
     * GET /api/held-orders/{id}
     */
    @GetMapping("/held-orders/{id}")
    public ResponseEntity<?> getHeldOrderById(@PathVariable String id,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return heldOrderRepository.findById(id)
                    .map(heldOrder -> {
                        if (!rid.equals(heldOrder.getRestaurantId())) {
                            return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Access denied"));
                        }
                        return ResponseEntity.ok(heldOrder);
                    })
                    .orElseGet(() -> ResponseEntity.status(404)
                            .body(Map.of("ok", false, "message", "Held order not found")));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    /**
     * Create held order (park order)
     * POST /api/held-orders
     */
    @PostMapping("/held-orders")
    public ResponseEntity<?> createHeldOrder(@RequestBody HeldOrder heldOrder,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            if (heldOrder.getId() == null || heldOrder.getId().isEmpty()) {
                heldOrder.setId(UUID.randomUUID().toString());
            }
            if (heldOrder.getCreatedAt() == null) {
                heldOrder.setCreatedAt(Instant.now().toString());
            }
            heldOrder.setRestaurantId(rid);

            HeldOrder saved = heldOrderRepository.save(heldOrder);

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("heldOrder", saved);
            response.put("count", heldOrderRepository.count());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Delete held order (retrieve or discard)
     * DELETE /api/held-orders/{id}
     */
    @DeleteMapping("/held-orders/{id}")
    public ResponseEntity<?> deleteHeldOrder(@PathVariable String id,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            HeldOrder heldOrder = heldOrderRepository.findById(id).orElse(null);
            if (heldOrder == null) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Held order not found"));
            }
            if (!rid.equals(heldOrder.getRestaurantId())) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Access denied"));
            }

            heldOrderRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Held order deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
