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
    private OrderService orderService;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/pos-orders")
    public List<Order> getOrders() {
        return orderService.getAllOrders();
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        try {
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
    public ResponseEntity<?> updateOrder(@PathVariable String id, @RequestBody Order updates) {
        Order updated = orderService.updateOrder(id, updates);
        if (updated != null) {
            return ResponseEntity.ok(Map.of("ok", true, "order", updated));
        }
        return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Order not found"));
    }

}
