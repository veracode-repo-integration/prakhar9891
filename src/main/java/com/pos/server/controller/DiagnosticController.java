package com.pos.server.controller;

import com.pos.server.model.User;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class DiagnosticController {

    @Autowired
    private AuthService authService;

    @GetMapping("/token-diagnostic")
    public ResponseEntity<?> getTokenDiagnostic() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Not authenticated"));
            }

            String username = auth.getName();
            User user = authService.findByUsername(username);

            Map<String, Object> diagnostic = new HashMap<>();
            diagnostic.put("ok", true);
            diagnostic.put("username", user.getUsername());
            diagnostic.put("firstName", user.getFirstName());
            diagnostic.put("role", user.getRole());
            diagnostic.put("restaurantId", user.getRestaurantId());
            diagnostic.put("deviceToken", user.getDeviceToken() != null ? user.getDeviceToken() : "MISSING");
            diagnostic.put("deviceTokenLength", user.getDeviceToken() != null ? user.getDeviceToken().length() : 0);
            diagnostic.put("serverTime", System.currentTimeMillis());

            return ResponseEntity.ok(diagnostic);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
