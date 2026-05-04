package com.pos.server.controller;

import com.pos.server.service.AuthService;
import com.pos.server.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")
public class RazorpayController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RazorpayService razorpayService;

    /**
     * Create a Razorpay Order.
     * Frontend calls this to get an order_id before opening checkout.
     *
     * Request body: { amount: 500.00, currency: "INR", receipt: "order_123", notes: { source: "dine_in" } }
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body,
                                         @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            // Validate auth — this endpoint needs a logged-in user OR is called from dine-in (public)
            // For dine-in, restaurantId comes from the scan URL; for POS, from the header
            if (!razorpayService.isConfigured()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Razorpay is not configured on the server."));
            }

            // Amount in rupees from frontend → convert to paise
            double amountRupees = Double.parseDouble(body.getOrDefault("amount", "0").toString());
            if (amountRupees <= 0) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Invalid amount."));
            }
            long amountPaise = Math.round(amountRupees * 100);

            String currency = (String) body.getOrDefault("currency", "INR");
            String receipt = (String) body.getOrDefault("receipt", null);

            // Extract notes
            Map<String, String> notes = new HashMap<>();
            if (body.containsKey("notes") && body.get("notes") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rawNotes = (Map<String, Object>) body.get("notes");
                rawNotes.forEach((k, v) -> notes.put(k, v != null ? v.toString() : ""));
            }
            if (restaurantId != null && !restaurantId.isEmpty()) {
                notes.put("restaurantId", restaurantId);
            }

            Map<String, Object> order = razorpayService.createOrder(amountPaise, currency, receipt, notes);

            Map<String, Object> response = new HashMap<>(order);
            response.put("ok", true);
            response.put("keyId", razorpayService.getKeyId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "ok", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to create Razorpay order"
            ));
        }
    }

    /**
     * Verify Razorpay payment signature.
     * Frontend calls this after checkout success to confirm authenticity.
     *
     * Request body: { razorpay_order_id, razorpay_payment_id, razorpay_signature }
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String orderId = body.get("razorpay_order_id");
            String paymentId = body.get("razorpay_payment_id");
            String signature = body.get("razorpay_signature");

            if (orderId == null || paymentId == null || signature == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Missing required payment fields."));
            }

            boolean valid = razorpayService.verifyPaymentSignature(orderId, paymentId, signature);

            if (valid) {
                return ResponseEntity.ok(Map.of(
                        "ok", true,
                        "verified", true,
                        "paymentId", paymentId,
                        "orderId", orderId
                ));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                        "ok", false,
                        "verified", false,
                        "message", "Payment signature verification failed."
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "ok", false,
                    "message", e.getMessage() != null ? e.getMessage() : "Payment verification failed"
            ));
        }
    }
}
