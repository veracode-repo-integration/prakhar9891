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
import java.util.UUID;

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
            Order order = new Order();
            order.setId("ORD-" + System.currentTimeMillis()); // Simple ID generation
            order.setItems(request.getCart());
            order.setCreatedAt(System.currentTimeMillis());
            order.setStatus("paid");

            if (request.getPaymentInfo() != null) {
                order.setMethod((String) request.getPaymentInfo().get("method"));
                order.setNote((String) request.getPaymentInfo().get("note"));
                // Serialize Objects to JSON strings for simple storage matches existing
                // structure
                order.setCustomerJson(mapper.writeValueAsString(request.getPaymentInfo().get("customer")));
            }
            if (request.getUser() != null) {
                order.setUserJson(mapper.writeValueAsString(request.getUser()));
            }

            // Calc total
            double total = request.getCart().stream().mapToDouble(i -> i.getPrice() * i.getQty()).sum();
            order.setTotal(total);

            Order saved = orderService.createOrder(order);
            return ResponseEntity.ok(Map.of("ok", true, "order", saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
