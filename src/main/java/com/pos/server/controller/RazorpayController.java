package com.pos.server.controller;

import com.pos.server.model.Settings;
import com.pos.server.repository.SettingsRepository;
import com.pos.server.service.AuthService;
import com.pos.server.service.CommonDataService;
import com.pos.server.service.RazorpayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private CommonDataService commonDataService;

    @Autowired
    private ObjectMapper objectMapper;

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
            if (restaurantId == null || restaurantId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Restaurant ID header required."));
            }

            Settings settings = commonDataService.getSettings(restaurantId);
            JsonNode rawJson = settings.getRawSettingsJson() != null ? objectMapper.readTree(settings.getRawSettingsJson()) : null;
            
            String rId = rawJson != null && rawJson.has("razorpayKeyId") ? rawJson.get("razorpayKeyId").asText() : null;
            String rSecret = rawJson != null && rawJson.has("razorpayKeySecret") ? rawJson.get("razorpayKeySecret").asText() : null;

            if (rId == null || rSecret == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Razorpay is not configured for this restaurant."));
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

            Map<String, Object> order = razorpayService.createOrder(amountPaise, currency, receipt, notes, rId, rSecret);

            Map<String, Object> response = new HashMap<>(order);
            response.put("ok", true);
            response.put("keyId", rId);
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
            if (restaurantId == null || restaurantId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Restaurant ID header required."));
            }

            Settings settings = commonDataService.getSettings(restaurantId);
            JsonNode rawJson = settings.getRawSettingsJson() != null ? objectMapper.readTree(settings.getRawSettingsJson()) : null;
            String rSecret = rawJson != null && rawJson.has("razorpayKeySecret") ? rawJson.get("razorpayKeySecret").asText() : null;

            if (rSecret == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Razorpay keys not found for this restaurant."));
            }

            String orderId = body.get("razorpay_order_id");
            String paymentId = body.get("razorpay_payment_id");
            String signature = body.get("razorpay_signature");

            if (orderId == null || paymentId == null || signature == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Missing required payment fields."));
            }

            boolean valid = razorpayService.verifyPaymentSignature(orderId, paymentId, signature, rSecret);

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
