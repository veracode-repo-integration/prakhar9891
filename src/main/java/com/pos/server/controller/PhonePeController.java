package com.pos.server.controller;

import com.pos.server.model.Settings;
import com.pos.server.repository.SettingsRepository;
import com.pos.server.service.CommonDataService;
import com.pos.server.service.PhonePeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/phonepe")
public class PhonePeController {

    @Autowired
    private PhonePeService phonePeService;

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private CommonDataService commonDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.pos.server.repository.OrderRepository orderRepository;

    @Autowired
    private com.pos.server.repository.PaymentDetailRepository paymentDetailRepository;

    /**
     * Prepare a PhonePe payment request.
     * Returns the Base64 payload and Checksum for the frontend.
     */
    @PostMapping("/initiate-payment")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> body,
                                             @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            if (restaurantId == null || restaurantId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Restaurant ID header required."));
            }

            Settings settings = commonDataService.getSettings(restaurantId);
            JsonNode rawJson = settings.getRawSettingsJson() != null ? objectMapper.readTree(settings.getRawSettingsJson()) : null;
            
            String mId = rawJson != null && rawJson.has("phonepeMerchantId") ? rawJson.get("phonepeMerchantId").asText() : null;
            String saltKey = rawJson != null && rawJson.has("phonepeSaltKey") ? rawJson.get("phonepeSaltKey").asText() : null;
            String saltIndex = rawJson != null && rawJson.has("phonepeSaltIndex") ? rawJson.get("phonepeSaltIndex").asText() : "1";

            if (mId == null || saltKey == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "PhonePe is not configured for this restaurant."));
            }

            String clientTxId = (String) body.get("merchantTransactionId");
            if (clientTxId == null || clientTxId.trim().isEmpty()) {
                clientTxId = "TXN_" + System.currentTimeMillis();
            }
            String txId = restaurantId + "_" + clientTxId;
            double amount = Double.parseDouble(body.getOrDefault("amount", "0").toString());
            String mobile = (String) body.get("mobileNumber");
            String callbackUrl = (String) body.get("callbackUrl");

            if (amount <= 0) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Invalid amount."));
            }

            long amountPaise = Math.round(amount * 100);
            String base64Payload = phonePeService.createPaymentPayload(mId, txId, amountPaise, callbackUrl, mobile);
            String checksum = phonePeService.generateXVerify(base64Payload, "/pg/v1/pay", saltKey, saltIndex);

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("base64Payload", base64Payload);
            response.put("checksum", checksum);
            response.put("merchantId", mId);
            response.put("baseUrl", phonePeService.getBaseUrl());
            response.put("merchantTransactionId", clientTxId); // Return client-side ID so it matches client expectations
            response.put("fullTransactionId", txId);
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    /**
     * Check payment status from PhonePe server.
     */
    @GetMapping("/status/{merchantTransactionId}")
    public ResponseEntity<?> checkStatus(@PathVariable String merchantTransactionId,
                                         @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            if (restaurantId == null || restaurantId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Restaurant ID header required."));
            }

            Settings settings = commonDataService.getSettings(restaurantId);
            JsonNode rawJson = settings.getRawSettingsJson() != null ? objectMapper.readTree(settings.getRawSettingsJson()) : null;
            
            String mId = rawJson != null && rawJson.has("phonepeMerchantId") ? rawJson.get("phonepeMerchantId").asText() : null;
            String saltKey = rawJson != null && rawJson.has("phonepeSaltKey") ? rawJson.get("phonepeSaltKey").asText() : null;
            String saltIndex = rawJson != null && rawJson.has("phonepeSaltIndex") ? rawJson.get("phonepeSaltIndex").asText() : "1";

            if (mId == null || saltKey == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "PhonePe keys not found for this restaurant."));
            }

            String fullTxId = merchantTransactionId.contains("_") ? merchantTransactionId : restaurantId + "_" + merchantTransactionId;
            String endpoint = "/pg/v1/status/" + mId + "/" + fullTxId;
            String xVerify = phonePeService.generateXVerify("", endpoint, saltKey, saltIndex);

            URL url = new URL(phonePeService.getBaseUrl() + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-VERIFY", xVerify);
            conn.setRequestProperty("X-MERCHANT-ID", mId);

            int responseCode = conn.getResponseCode();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream()
            ));
            
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return ResponseEntity.status(responseCode).body(response.toString());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    /**
     * Automated Callback from PhonePe.
     * Note: PhonePe sends Base64 encoded payload in the POST body.
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-VERIFY", required = false) String xVerify) {
        try {
            System.out.println("[PhonePe] Received Webhook Callback: " + body);
            String base64Response = body.get("response");
            if (base64Response == null) {
                return ResponseEntity.badRequest().body("Missing response body parameter");
            }

            // 1. Decode payload to extract transaction metadata (restaurant ID prefix)
            String decodedJson = new String(java.util.Base64.getDecoder().decode(base64Response), java.nio.charset.StandardCharsets.UTF_8);
            JsonNode decodedNode = objectMapper.readTree(decodedJson);
            JsonNode dataNode = decodedNode.get("data");
            
            if (dataNode == null || !dataNode.has("merchantTransactionId")) {
                return ResponseEntity.badRequest().body("Missing merchantTransactionId inside callback data");
            }
            
            String fullTxId = dataNode.get("merchantTransactionId").asText();
            String restaurantId = "";
            String clientTxId = fullTxId;
            
            if (fullTxId.contains("_")) {
                int underscoreIndex = fullTxId.indexOf("_");
                restaurantId = fullTxId.substring(0, underscoreIndex);
                clientTxId = fullTxId.substring(underscoreIndex + 1);
            }

            // 2. Fetch settings and keys for signature verification
            Settings settings = commonDataService.getSettings(restaurantId);
            JsonNode settingsJson = settings.getRawSettingsJson() != null ? objectMapper.readTree(settings.getRawSettingsJson()) : null;
            String saltKey = settingsJson != null && settingsJson.has("phonepeSaltKey") ? settingsJson.get("phonepeSaltKey").asText() : null;
            String saltIndex = settingsJson != null && settingsJson.has("phonepeSaltIndex") ? settingsJson.get("phonepeSaltIndex").asText() : "1";

            if (saltKey == null) {
                System.err.println("[PhonePe] Webhook REJECTED: Salt key not configured for restaurant: " + restaurantId);
                return ResponseEntity.status(400).body("PhonePe credentials not configured for restaurant");
            }

            // 3. Verify callback checksum signature
            boolean isVerified = phonePeService.verifyCallback(base64Response, xVerify, saltKey, saltIndex);
            if (!isVerified) {
                System.err.println("[PhonePe] Webhook REJECTED: Invalid checksum header for transaction: " + fullTxId);
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // 4. Update Database on Payment Success
            boolean success = decodedNode.has("success") && decodedNode.get("success").asBoolean();
            String code = decodedNode.has("code") ? decodedNode.get("code").asText() : "";
            
            if (success && "PAYMENT_SUCCESS".equalsIgnoreCase(code)) {
                System.out.println("[PhonePe] Webhook Success verified for: " + fullTxId + ". Settling order in DB.");
                
                // Lookup payment detail records matching client reference or full reference
                List<com.pos.server.model.PaymentDetail> details = paymentDetailRepository.findAllByReference(clientTxId);
                if (details.isEmpty()) {
                    details = paymentDetailRepository.findAllByReference(fullTxId);
                }
                
                for (com.pos.server.model.PaymentDetail detail : details) {
                    if (detail.getOrder() != null) {
                        com.pos.server.model.Order order = detail.getOrder();
                        order.setStatus("completed");
                        orderRepository.save(order);
                        System.out.println("[PhonePe] Webhook updated order ID: " + order.getId() + " status to completed.");
                    }
                }
            } else {
                System.out.println("[PhonePe] Webhook received non-success callback for: " + fullTxId + " Code: " + code);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            System.err.println("[PhonePe] Error handling webhook callback: " + e.getMessage());
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
