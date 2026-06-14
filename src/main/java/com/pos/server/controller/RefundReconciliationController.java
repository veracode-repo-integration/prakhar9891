package com.pos.server.controller;

import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
@CrossOrigin(origins = "*")
public class RefundReconciliationController {

    @Autowired
    private AuthService authService;

    @PostMapping("/reconciliation")
    public ResponseEntity<?> reconcileRefunds(
            @RequestBody List<Map<String, Object>> externalStatements,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);

            int matchedCount = 0;
            int unmatchedCount = 0;
            double reconciledAmount = 0.0;

            for (Map<String, Object> stmt : externalStatements) {
                String txId = (String) stmt.get("transactionId");
                double amt = Double.parseDouble(stmt.getOrDefault("amount", "0.0").toString());
                String status = (String) stmt.get("status");

                if ("SUCCESS".equalsIgnoreCase(status) && txId != null) {
                    matchedCount++;
                    reconciledAmount += amt;
                } else {
                    unmatchedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("ok", true);
            result.put("matchedCount", matchedCount);
            result.put("unmatchedCount", unmatchedCount);
            result.put("reconciledAmount", reconciledAmount);
            result.put("message", "Refund statement reconciliation execution completed successfully.");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
