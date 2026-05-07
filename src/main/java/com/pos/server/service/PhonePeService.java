package com.pos.server.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class PhonePeService {

    @Value("${PHONEPE_MERCHANT_ID:}")
    private String merchantId;

    @Value("${PHONEPE_SALT_KEY:}")
    private String saltKey;

    @Value("${PHONEPE_SALT_INDEX:1}")
    private String saltIndex;

    @Value("${PHONEPE_ENV:PRODUCTION}") // UAT or PRODUCTION
    private String environment;

    private static final String UAT_URL = "https://api-preprod.phonepe.com/apis/pg-sandbox";
    private static final String PROD_URL = "https://api.phonepe.com/apis/hermes";

    public String getBaseUrl() {
        return "PRODUCTION".equalsIgnoreCase(environment) ? PROD_URL : UAT_URL;
    }

    /**
     * Generate the X-VERIFY header for PhonePe API requests.
     * Formula: SHA256(Base64Payload + endpoint + saltKey) + "###" + saltIndex
     */
    public String generateXVerify(String base64Payload, String endpoint) {
        try {
            String stringToHash = base64Payload + endpoint + saltKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString() + "###" + saltIndex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PhonePe X-VERIFY header", e);
        }
    }

    /**
     * Create a Base64 encoded JSON payload for the payment request.
     */
    public String createPaymentPayload(String merchantTransactionId, long amountPaise, String callbackUrl, String mobileNumber) {
        JSONObject payload = new JSONObject();
        payload.put("merchantId", merchantId);
        payload.put("merchantTransactionId", merchantTransactionId);
        payload.put("merchantUserId", "USER_" + mobileNumber);
        payload.put("amount", amountPaise);
        payload.put("redirectUrl", callbackUrl); // For intent flow, we can use this for polling
        payload.put("redirectMode", "POST");
        payload.put("callbackUrl", callbackUrl);
        payload.put("mobileNumber", mobileNumber);
        
        JSONObject paymentInstrument = new JSONObject();
        paymentInstrument.put("type", "PAY_PAGE"); // Default Pay Page, mobile handles intent
        payload.put("paymentInstrument", paymentInstrument);

        return Base64.getEncoder().encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    public String getMerchantId() {
        return merchantId;
    }

    public boolean isConfigured() {
        return merchantId != null && !merchantId.isEmpty() && saltKey != null && !saltKey.isEmpty();
    }
}
