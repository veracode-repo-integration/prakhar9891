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
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private com.pos.server.service.AuthService authService;

    @Autowired
    private com.pos.server.service.OrderService orderService;

    @GetMapping("/pos-orders")
    public List<Order> getOrders(@RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String rid = validateRestaurantAccess(restaurantId);
        return orderService.getAllOrders(rid);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = validateRestaurantAccess(restaurantId);
            request.setRestaurantId(rid);

            Order saved = orderService.processCheckout(request);
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
            String rid = validateRestaurantAccess(restaurantId);
            Order updated = orderService.updateOrder(id, updates, rid);
            if (updated != null) {
                return ResponseEntity.ok(Map.of("ok", true, "order", updated));
            }
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Order not found"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    private String validateRestaurantAccess(String headerId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // For public/kiosk access if needed, but for now we require auth as per
            // SecurityConfig
            throw new RuntimeException("Unauthorized");
        }

        String username = auth.getName();
        com.pos.server.model.User user = authService.findByUsername(username);

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            // Admin can access any restaurant if specified, otherwise their own
            if (headerId != null && !headerId.trim().isEmpty()) {
                return headerId.toLowerCase().trim();
            }
            return user.getRestaurantId() != null ? user.getRestaurantId().toLowerCase().trim() : null;
        }

        // Non-Admins are STRICTLY bound to their assigned restaurant
        if (user.getRestaurantId() == null || user.getRestaurantId().isEmpty()) {
            throw new RuntimeException("User not assigned to any restaurant");
        }

        return user.getRestaurantId().toLowerCase().trim();
    }

}
