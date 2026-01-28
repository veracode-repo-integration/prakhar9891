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

    /**
     * Get all held orders
     * GET /api/held-orders
     */
    @GetMapping("/held-orders")
    public ResponseEntity<List<HeldOrder>> getAllHeldOrders() {
        return ResponseEntity.ok(heldOrderRepository.findAll());
    }

    /**
     * Get held order by ID
     * GET /api/held-orders/{id}
     */
    @GetMapping("/held-orders/{id}")
    public ResponseEntity<?> getHeldOrderById(@PathVariable String id) {
        return heldOrderRepository.findById(id)
                .<ResponseEntity<?>>map(heldOrder -> ResponseEntity.ok(heldOrder))
                .orElseGet(
                        () -> ResponseEntity.status(404).body(Map.of("ok", false, "message", "Held order not found")));
    }

    /**
     * Create held order (park order)
     * POST /api/held-orders
     */
    @PostMapping("/held-orders")
    public ResponseEntity<?> createHeldOrder(@RequestBody HeldOrder heldOrder) {
        try {
            if (heldOrder.getId() == null || heldOrder.getId().isEmpty()) {
                heldOrder.setId(String.valueOf(System.currentTimeMillis()));
            }
            if (heldOrder.getCreatedAt() == null) {
                heldOrder.setCreatedAt(Instant.now().toString());
            }

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
    public ResponseEntity<?> deleteHeldOrder(@PathVariable String id) {
        try {
            if (!heldOrderRepository.existsById(id)) {
                Map<String, Object> error = new HashMap<>();
                error.put("ok", false);
                error.put("message", "Held order not found");
                return ResponseEntity.status(404).body(error);
            }

            heldOrderRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("message", "Held order deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("ok", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
