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
import java.util.HashMap;

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

    @Autowired
    private com.pos.server.service.CommonDataService commonDataService;

    @org.springframework.beans.factory.annotation.Value("${pos.integration.ghartak.secret:GHARTAK_SECURE_WEBHOOK_KEY_2026}")
    private String ghartakSecret;



    @GetMapping("/v2/pos-orders/test-push")
    public ResponseEntity<String> testPush(@RequestParam String restaurantId) {
        try {
            System.out.println("[FORENSIC] [TEST-PUSH] START for RID: [" + restaurantId + "]");
            List<String> tokens = authService.findTokensByRestaurantIdSmart(restaurantId);

            if (tokens.isEmpty()) {
                System.err.println("[FORENSIC] [TEST-PUSH] [FAIL] No tokens found for RID: " + restaurantId);
                return ResponseEntity.ok("No tokens found for [" + restaurantId + "] in Database. Checked Smart Match.");
            }

            System.out.println("[FORENSIC] [TEST-PUSH] [SUCCESS] Dispatching to " + tokens.size() + " devices.");
            fcmService.dispatchHighPrioritySiren(tokens, "Siren Test", "Manual Siren Triggered for " + restaurantId, Map.of("type", "TEST"));
            return ResponseEntity.ok("Push sent to " + tokens.size() + " devices! Unified Elite logic used.");
        } catch (Exception e) {
            System.err.println("[FORENSIC] [TEST-PUSH] [ERROR] " + e.getMessage());
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

            // Enforce table containment for guests during checkout
            if (request.getTableId() != null && !request.getTableId().trim().isEmpty()) {
                authService.validateTableAccess(request.getTableId());
            }

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
                // If it is a Ghar Tak order and status is ready, preparing, or cancelled, notify Ghar Tak
                if (updated.getIsOnline() != null && updated.getIsOnline() && 
                    "GHARTAK".equalsIgnoreCase(updated.getAggregatorName())) {
                    notifyGharTakStatusUpdate(updated);
                }

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

    @GetMapping("/v2/pos-orders/public/invoice/{invoiceId}")
    public ResponseEntity<?> getPublicInvoice(
            @PathVariable String invoiceId,
            @RequestParam(value = "token", required = false) String token) {
        try {
            Order order = orderService.getOrderByInvoiceNumber(invoiceId);
            if (order == null) {
                return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Invoice not found"));
            }

            // Security: Validate access token if the order has one
            // Legacy orders (without accessToken) are allowed through for backward compatibility
            if (order.getAccessToken() != null && !order.getAccessToken().isEmpty()) {
                if (token == null || !token.equals(order.getAccessToken())) {
                    return ResponseEntity.status(403).body(Map.of(
                        "ok", false,
                        "message", "Access denied. Invalid or missing invoice token."
                    ));
                }
            }

            com.pos.server.model.Settings settings = commonDataService.getSettings(order.getRestaurantId());

            double itemsGross = order.getItems().stream().mapToDouble(it -> it.getPrice() * it.getQty()).sum();
            double discountAmount = order.getDiscountValue();
            if ("percent".equalsIgnoreCase(order.getDiscountType())) {
                discountAmount = itemsGross * (order.getDiscountValue() / 100.0);
            }

            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (com.pos.server.model.OrderItem it : order.getItems()) {
                items.add(Map.of(
                    "n", it.getName() != null ? it.getName() : "",
                    "q", it.getQty(),
                    "p", it.getPrice()
                ));
            }

            String contactNumber = "";
            try {
                if (settings.getRawSettingsJson() != null && !settings.getRawSettingsJson().isEmpty()) {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(settings.getRawSettingsJson());
                    if (node.has("contactNumber")) {
                        contactNumber = node.get("contactNumber").asText();
                    }
                }
            } catch (Exception e) {}

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("n", settings.getMerchantName() != null ? settings.getMerchantName() : "OJAS POS");
            response.put("a", settings.getAddress() != null ? settings.getAddress() : "");
            response.put("p", contactNumber);
            response.put("g", settings.getGstNumber() != null ? settings.getGstNumber() : "");
            response.put("i", order.getInvoiceNumber() != null ? order.getInvoiceNumber() : order.getId());
            response.put("d", order.getCreatedAt());
            response.put("m", order.getMethod() != null ? order.getMethod() : "CASH");
            response.put("c", order.getCustomer() != null ? order.getCustomer().getName() : "Walk-in Guest");
            response.put("cm", order.getCustomer() != null ? order.getCustomer().getMobile() : "");
            boolean isTaxInclusive = Boolean.TRUE.equals(order.getIsTaxInclusive());
            double subtotalVal = order.getSubtotal();
            double taxVal = order.getTaxAmount() != null ? order.getTaxAmount() : 0.0;
            // Defensive correction: if tax is inclusive, but subtotal in DB was saved equal to itemsGross,
            // we dynamically correct it to the true tax-exclusive base.
            if (isTaxInclusive && Math.abs(subtotalVal - itemsGross) < 0.05) {
                subtotalVal = itemsGross - discountAmount - taxVal;
            }
            response.put("s", subtotalVal);
            response.put("inc", isTaxInclusive);
            response.put("x", taxVal);
            response.put("t", order.getTotal());
            response.put("cgst", order.getCgstAmount() != null ? order.getCgstAmount() : taxVal / 2.0);
            response.put("sgst", order.getSgstAmount() != null ? order.getSgstAmount() : taxVal / 2.0);
            
            double baseForRate = subtotalVal - discountAmount;
            double calculatedCgstRate = 0.0;
            double calculatedSgstRate = 0.0;
            if (baseForRate > 0) {
                double cgstAmt = order.getCgstAmount() != null ? order.getCgstAmount() : taxVal / 2.0;
                double sgstAmt = order.getSgstAmount() != null ? order.getSgstAmount() : taxVal / 2.0;
                calculatedCgstRate = (cgstAmt / baseForRate) * 100.0;
                calculatedSgstRate = (sgstAmt / baseForRate) * 100.0;
            }
            response.put("cgstRate", calculatedCgstRate);
            response.put("sgstRate", calculatedSgstRate);
            response.put("it", items);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    private void notifyGharTakStatusUpdate(Order order) {
        new Thread(() -> {
            try {
                String webhookUrl = "https://api.ghartak.com/webhooks/order-status"; // Mock URL
                
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getExternalId() != null ? order.getExternalId() : order.getId());
                
                String statusVal = order.getKitchenStatus();
                if ("cancelled".equalsIgnoreCase(order.getStatus())) {
                    statusVal = "cancelled";
                }
                payload.put("status", statusVal); // "preparing", "ready", or "cancelled"
                
                String jsonPayload = objectMapper.writeValueAsString(payload);
                System.out.println("[GHARTAK-WEBHOOK] Outgoing Notification: URL=" + webhookUrl + " | Body=" + jsonPayload);
                
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(webhookUrl))
                        .header("Content-Type", "application/json");
                if (ghartakSecret != null) {
                    builder.header("X-Integration-API-Key", ghartakSecret);
                }
                java.net.http.HttpRequest request = builder
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .build();
                        
                java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                System.out.println("[GHARTAK-WEBHOOK] Response code: " + response.statusCode() + " | Body: " + response.body());
            } catch (Exception e) {
                System.err.println("[GHARTAK-WEBHOOK] Failed to send status update: " + e.getMessage());
            }
        }).start();

    }

}
