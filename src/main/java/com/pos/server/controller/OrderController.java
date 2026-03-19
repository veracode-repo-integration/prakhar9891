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
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @GetMapping("/pos-orders")
    public ResponseEntity<List<Order>> getOrders(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(orderService.getAllOrders(rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }

    @GetMapping("/v2/pos-orders")
    public ResponseEntity<org.springframework.data.domain.Page<Order>> getOrdersPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(orderService.getOrdersPaged(rid, page, size));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(org.springframework.data.domain.Page.empty());
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
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }


}
