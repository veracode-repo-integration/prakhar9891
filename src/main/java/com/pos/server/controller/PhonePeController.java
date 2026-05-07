package com.pos.server.controller;

import com.pos.server.service.PhonePeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/phonepe")
public class PhonePeController {

    @Autowired
    private PhonePeService phonePeService;

    /**
     * Prepare a PhonePe payment request.
     * Returns the Base64 payload and Checksum for the frontend.
     */
    @PostMapping("/initiate-payment")
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, Object> body) {
        try {
            if (!phonePeService.isConfigured()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "PhonePe is not configured on the server."));
            }

            String txId = (String) body.get("merchantTransactionId");
            double amount = Double.parseDouble(body.getOrDefault("amount", "0").toString());
            String mobile = (String) body.get("mobileNumber");
            String callbackUrl = (String) body.get("callbackUrl");

            if (txId == null || amount <= 0) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Missing transaction ID or invalid amount."));
            }

            long amountPaise = Math.round(amount * 100);
            String base64Payload = phonePeService.createPaymentPayload(txId, amountPaise, callbackUrl, mobile);
            String checksum = phonePeService.generateXVerify(base64Payload, "/pg/v1/pay");

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("base64Payload", base64Payload);
            response.put("checksum", checksum);
            response.put("merchantId", phonePeService.getMerchantId());
            response.put("baseUrl", phonePeService.getBaseUrl());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    /**
     * Check payment status from PhonePe server.
     */
    @GetMapping("/status/{merchantTransactionId}")
    public ResponseEntity<?> checkStatus(@PathVariable String merchantTransactionId) {
        try {
            String merchantId = phonePeService.getMerchantId();
            String endpoint = "/pg/v1/status/" + merchantId + "/" + merchantTransactionId;
            String xVerify = phonePeService.generateXVerify("", endpoint); // Base64 is empty for GET status

            URL url = new URL(phonePeService.getBaseUrl() + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-VERIFY", xVerify);
            conn.setRequestProperty("X-MERCHANT-ID", merchantId);

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
    public ResponseEntity<?> handleCallback(@RequestBody String base64Response) {
        // In a real scenario, we would decode this, verify the checksum, 
        // and update the order/table status in the database.
        // For now, we acknowledge receipt.
        System.out.println("[PhonePe] Received Callback: " + base64Response);
        return ResponseEntity.ok("OK");
    }
}
