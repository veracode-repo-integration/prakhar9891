package com.pos.server.controller;

import com.pos.server.dto.LoginRequest;
import com.pos.server.model.User;
import com.pos.server.security.JwtUtils;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private com.pos.server.repository.RestaurantRepository restaurantRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(org.springframework.security.core.Authentication auth) {
        if (auth == null)
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));

        Map<String, Object> details = new java.util.HashMap<>();
        details.put("username", auth.getName());
        details.put("authorities", auth.getAuthorities());

        try {
            User user = authService.findByUsername(auth.getName());
            if (user != null) {
                details.put("dbRole", user.getRole());
            }
        } catch (Exception e) {
        }

        return ResponseEntity.ok(details);
    }

    private com.pos.server.dto.UserResponse createUserResponse(User user) {
        java.util.List<String> allowedFeatures = new java.util.ArrayList<>();
        String onlineStatus = "OPEN";
        String restaurantStatus = "ACTIVE";
        if (user.getRestaurantId() != null) {
            com.pos.server.model.Restaurant r = restaurantRepository.findById(user.getRestaurantId()).orElse(null);
            if (r != null) {
                if (r.getAllowedFeatures() != null) {
                    allowedFeatures.addAll(r.getAllowedFeatures());
                }
                onlineStatus = r.getOnlineStatus() != null ? r.getOnlineStatus() : "OPEN";
                restaurantStatus = r.getStatus() != null ? r.getStatus() : "ACTIVE";
            }
        }
        return new com.pos.server.dto.UserResponse(
                user.getDbId(),
                user.getUsername(),
                (user.getFirstName() != null ? user.getFirstName() : user.getUsername()),
                user.getEmail(),
                user.getContact(),
                user.getRole(),
                user.getRestaurantId(),
                onlineStatus,
                allowedFeatures,
                restaurantStatus);
    }

    @GetMapping("/check-user")
    public ResponseEntity<?> checkUser(@RequestParam String phone) {
        boolean exists = authService.checkUserExistsByPhone(phone);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody com.pos.server.dto.RegisterRequest request) {
        try {
            User user = authService.register(request);
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getRestaurantId());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", createUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@javax.validation.Valid @RequestBody LoginRequest request) {
        try {
            // identifier, passwordHash (raw password from frontend)
            User user = authService.login(request.getIdentifier(), request.getPasswordHash());
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getRestaurantId());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", createUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("suspended")) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Account is suspended"));
            }
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Invalid credentials"));
        }
    }

    @PostMapping("/otp-login")
    public ResponseEntity<?> otpLogin(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");
            String restaurantId = request.get("restaurantId");
            String name = request.get("name");
            User user = authService.loginByPhone(idToken, restaurantId, name);
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getRestaurantId());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", createUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("suspended")) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Account is suspended"));
            }
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/guest-login")
    public ResponseEntity<?> guestLogin(@RequestBody Map<String, String> request) {
        try {
            String phone = request.get("phone");
            String restaurantId = request.get("restaurantId");
            String name = request.get("name");
            String tableId = request.get("tableId");
            
            // For UPI-First Dine-In, we skip Firebase OTP
            User user = authService.loginByPhone("guest-otp-bypass", restaurantId, phone);
            // Optionally update name if provided
            if (name != null && !name.isEmpty()) {
                user.setFirstName(name);
                authService.saveUser(user);
            }
            
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getRestaurantId(), tableId);

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", createUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }


    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            String username = auth.getName();
            String sessionRole = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst().orElse("STAFF");

            User user = authService.findByUsername(username);
            // Override user role with session role from JWT
            user.setRole(sessionRole);

            return ResponseEntity.ok(createUserResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Unauthorized"));
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            String username = auth.getName();
            authService.deleteUser(username);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Account deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/onboarding")
    public ResponseEntity<?> submitOnboarding(@RequestBody Map<String, String> data) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Not authenticated"));
            }
            String username = auth.getName();
            User user = authService.findByUsername(username);
            
            Map<String, Object> response = authService.submitOnboarding(user, data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> updates) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
            }
            String username = auth.getName();
            User user = authService.findByUsername(username);

            // Handle onlineStatus (Restaurant Level)
            if (updates.containsKey("onlineStatus") && user.getRestaurantId() != null) {
                String newStatus = (String) updates.get("onlineStatus");
                restaurantRepository.findById(user.getRestaurantId()).ifPresent(r -> {
                    r.setOnlineStatus(newStatus);
                    restaurantRepository.save(r);
                    System.out.println("[Auth] Updated OnlineStatus for " + r.getId() + " to " + newStatus);
                });
            }

            // Handle other field updates (User Level)
            if (updates.containsKey("firstName")) user.setFirstName((String) updates.get("firstName"));
            if (updates.containsKey("lastName")) user.setLastName((String) updates.get("lastName"));
            if (updates.containsKey("email")) user.setEmail((String) updates.get("email"));
            
            authService.saveUser(user);
            return ResponseEntity.ok(Map.of("ok", true, "user", createUserResponse(user)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/update-device-token")
    public ResponseEntity<?> updateDeviceToken(@RequestBody Map<String, String> request) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                System.err.println("[Auth] Token update FAILED: Not authenticated");
                return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Not authenticated"));
            }
            String username = auth.getName();
            String token = request.get("token");
            if (token != null) {
                System.out.println("[Auth] REGISTERING DEVICE TOKEN for " + username + ": " + token);
                authService.updateDeviceToken(username, token);
                return ResponseEntity.ok(Map.of("ok", true, "message", "Token updated"));
            }
            System.err.println("[Auth] Token update FAILED: Token missing in request for " + username);
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Token missing"));
        } catch (Exception e) {
            System.err.println("[Auth] Token update ERROR: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @Autowired
    private com.pos.server.service.TokenBlacklistService tokenBlacklistService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            long expiry = jwtUtils.getExpirationTimeFromToken(token);
            tokenBlacklistService.blacklistToken(token, expiry);
        }
        return ResponseEntity.ok(Map.of("ok", true, "message", "Logged out successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
            }
            String username = auth.getName();
            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");
            
            authService.changePassword(username, currentPassword, newPassword);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
