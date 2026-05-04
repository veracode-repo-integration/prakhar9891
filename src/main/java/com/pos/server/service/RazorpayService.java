package com.pos.server.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

@Service
public class RazorpayService {

    @Value("${RAZORPAY_KEY_ID:}")
    private String keyId;

    @Value("${RAZORPAY_KEY_SECRET:}")
    private String keySecret;

    @Value("${RAZORPAY_PLATFORM_COMMISSION_PERCENT:2}")
    private double platformCommissionPercent;

    private RazorpayClient client;

    @PostConstruct
    public void init() {
        try {
            if (keyId != null && !keyId.isEmpty() && keySecret != null && !keySecret.isEmpty()) {
                this.client = new RazorpayClient(keyId, keySecret);
                System.out.println("[RazorpayService] Initialized with key: " + keyId.substring(0, Math.min(12, keyId.length())) + "...");
            } else {
                System.out.println("[RazorpayService] No keys configured — Razorpay disabled.");
            }
        } catch (RazorpayException e) {
            System.err.println("[RazorpayService] Failed to initialize: " + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return client != null;
    }

    /**
     * Create a Razorpay Order.
     *
     * @param amountPaise Amount in paise (e.g., 50000 = ₹500)
     * @param currency    Currency code (default INR)
     * @param receipt     Unique receipt/reference ID
     * @param notes       Optional metadata
     * @return Map with orderId, amount, currency, status
     */
    public Map<String, Object> createOrder(long amountPaise, String currency, String receipt, Map<String, String> notes) throws RazorpayException {
        if (client == null) {
            throw new IllegalStateException("Razorpay is not configured. Please set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountPaise);
        orderRequest.put("currency", currency != null ? currency : "INR");
        orderRequest.put("receipt", receipt != null ? receipt : "rcpt_" + System.currentTimeMillis());

        if (notes != null && !notes.isEmpty()) {
            JSONObject notesJson = new JSONObject(notes);
            orderRequest.put("notes", notesJson);
        }

        Order order = client.orders.create(orderRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.get("id"));
        result.put("amount", order.get("amount"));
        result.put("currency", order.get("currency"));
        result.put("status", order.get("status"));
        return result;
    }

    /**
     * Verify payment signature using HMAC-SHA256.
     * Razorpay Standard Checkout sends: razorpay_order_id, razorpay_payment_id, razorpay_signature
     *
     * @return true if signature is valid
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keySecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());

            // Convert to hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);
        } catch (Exception e) {
            System.err.println("[RazorpayService] Signature verification error: " + e.getMessage());
            return false;
        }
    }

    public double getPlatformCommissionPercent() {
        return platformCommissionPercent;
    }

    public String getKeyId() {
        return keyId;
    }
}
