package com.pos.server.controller;

import com.pos.server.dto.CheckoutRequest;
import com.pos.server.model.Order;
import com.pos.server.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private com.pos.server.service.AuthService authService;

    @Autowired
    private com.pos.server.service.OrderService orderService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired
    private com.pos.server.service.FcmService fcmService;

    @Autowired
    private com.pos.server.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @GetMapping("/v2/pos-orders/test-push")
    public ResponseEntity<String> testPush(@RequestParam String restaurantId) {
        try {
            System.out.println("[FORENSIC] Testing push for restaurantId: [" + restaurantId + "]");
            List<String> tokens = new java.util.ArrayList<>();
            
            userRepository.findAll().forEach(u -> {
                String uRid = (u.getRestaurantId() != null) ? u.getRestaurantId().trim() : "";
                String targetRid = (restaurantId != null) ? restaurantId.trim() : "";
                
                if (uRid.equalsIgnoreCase(targetRid)) {
                    if (u.getDeviceToken() != null && !u.getDeviceToken().isEmpty()) {
                        tokens.add(u.getDeviceToken());
                    }
                }
            });

            if (tokens.isEmpty()) {
                return ResponseEntity.ok("No tokens found for [" + restaurantId + "] in Database. Try /api/v2/pos-orders/test-global to hit everyone!");
            }

            fcmService.sendPushNotification(tokens, "Siren Test", "Manual Siren Triggered for " + restaurantId, Map.of("type", "TEST"));
            return ResponseEntity.ok("Push sent to " + tokens.size() + " devices!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/v2/pos-orders/test-global")
    public ResponseEntity<String> testGlobal() {
        try {
            System.out.println("[FORENSIC] [GLOBAL] Pushing to EVERYONE in DB!");
            List<String> tokens = new java.util.ArrayList<>();
            
            userRepository.findAll().forEach(u -> {
                if (u.getDeviceToken() != null && !u.getDeviceToken().isEmpty()) {
                    tokens.add(u.getDeviceToken());
                }
            });

            if (tokens.isEmpty()) {
                return ResponseEntity.ok("Database is completely EMPTY of tokens!");
            }

            fcmService.sendPushNotification(tokens, "GLOBAL SIREN", "Testing all registered devices...", Map.of("type", "TEST"));
            return ResponseEntity.ok("GLOBAL Push sent to " + tokens.size() + " devices!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/pos-orders")
    public ResponseEntity<?> getOrders(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(orderService.getAllOrders(rid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage() != null ? e.getMessage() : "Unknown Error"));
        }
    }

    @GetMapping("/v2/pos-orders")
    public ResponseEntity<?> getOrdersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(orderService.getOrdersPaged(rid, page, size));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage() != null ? e.getMessage() : "Unknown Error"));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            request.setRestaurantId(rid);

            Order saved = orderService.processCheckout(request);
            // Notify subscribers about new order with acting user context (Defensive)
            org.springframework.security.core.Authentication authInfo = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            String actingUser = (authInfo != null) ? authInfo.getName() : "system";

            messagingTemplate.convertAndSend("/topic/orders", Map.of(
                    "order", saved,
                    "actingUser", actingUser,
                    "type", "NEW_ORDER"));
            return ResponseEntity.ok(Map.of("ok", true, "order", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("ok", false, "message", "Error processing checkout: " + e.getMessage()));
        }
    }

    @PutMapping("/pos-orders/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable String id, @RequestBody Order updates,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            Order updated = orderService.updateOrder(id, updates, rid);
            if (updated != null) {
                // Notify subscribers about updated order (Defensive)
                org.springframework.security.core.Authentication authInfo = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
                String actingUser = (authInfo != null) ? authInfo.getName() : "system";

                messagingTemplate.convertAndSend("/topic/orders", Map.of(
                        "order", updated,
                        "actingUser", actingUser,
                        "type", "ORDER_UPDATE"));
                return ResponseEntity.ok(Map.of("ok", true, "order", updated));
            }
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Order not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
    @PostMapping("/pos-orders/{id}/refund")
    public ResponseEntity<?> refundOrder(@PathVariable String id, @RequestBody Order updates,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            
            // Default status to refunded if not explicitly provided
            if (updates.getStatus() == null) {
                updates.setStatus("refunded");
            }
            
            Order updated = orderService.updateOrder(id, updates, rid);
            if (updated != null) {
                org.springframework.security.core.Authentication authInfo = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
                String actingUser = (authInfo != null) ? authInfo.getName() : "system";

                messagingTemplate.convertAndSend("/topic/orders", Map.of(
                        "order", updated,
                        "actingUser", actingUser,
                        "type", "ORDER_REFUND"));
                return ResponseEntity.ok(Map.of("ok", true, "order", updated));
            }
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Order not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

}
