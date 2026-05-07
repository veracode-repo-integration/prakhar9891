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

    @Value("${RAZORPAY_PLATFORM_COMMISSION_PERCENT:2}")
    private double platformCommissionPercent;

    /**
     * Create a dynamic client for a specific merchant.
     */
    private RazorpayClient getClient(String keyId, String keySecret) throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
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
    public Map<String, Object> createOrder(long amountPaise, String currency, String receipt, Map<String, String> notes, String keyId, String keySecret) throws RazorpayException {
        if (keyId == null || keySecret == null) {
            throw new IllegalStateException("Razorpay keys are missing for this merchant.");
        }

        RazorpayClient client = getClient(keyId, keySecret);

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
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature, String keySecret) {
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
