package com.pos.server.controller;

import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AuthService authService;

    /**
     * Notify the merchant about a successful payment.
     * This triggers the loud 'SUCCESS' chime on the Merchant POS.
     */
    @PostMapping("/broadcast-confirmation")
    public ResponseEntity<?> broadcastConfirmation(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        
        try {
            // Validate that we have a restaurant ID
            if (restaurantId == null || restaurantId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Restaurant ID required"));
            }

            // Prepare the notification message
            Map<String, Object> message = Map.of(
                "type", "PAYMENT_VERIFIED",
                "restaurantId", restaurantId,
                "tableId", payload.getOrDefault("tableId", ""),
                "tableName", payload.getOrDefault("tableName", ""),
                "amount", payload.getOrDefault("amount", 0),
                "method", payload.getOrDefault("method", "UPI"),
                "timestamp", System.currentTimeMillis()
            );

            // Broadcast to all connected clients (Dashboard, Kitchen, POS)
            messagingTemplate.convertAndSend("/topic/orders", message);

            return ResponseEntity.ok(Map.of("ok", true, "message", "Confirmation broadcasted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
