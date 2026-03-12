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
        if (user.getRestaurantId() != null) {
            restaurantRepository.findById(user.getRestaurantId()).ifPresent(r -> {
                if (r.getAllowedFeatures() != null) {
                    allowedFeatures.addAll(r.getAllowedFeatures());
                }
            });
        }
        return new com.pos.server.dto.UserResponse(
                user.getDbId(),
                user.getUsername(),
                (user.getFirstName() != null ? user.getFirstName() : user.getUsername()),
                user.getEmail(),
                user.getContact(),
                user.getRole(),
                user.getRestaurantId(),
                allowedFeatures);
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("suspended")) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "message", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", msg));
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("suspended")) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "message", msg));
            }
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Invalid credentials"));
        }
    }

    @PostMapping("/otp-login")
    public ResponseEntity<?> otpLogin(@RequestBody Map<String, String> request) {
        try {
            String phone = request.get("phone");
            String restaurantId = request.get("restaurantId");
            String name = request.get("name");
            User user = authService.loginByPhone(phone, restaurantId, name);
            String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getRestaurantId());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", createUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("suspended")) {
                return ResponseEntity.status(403).body(Map.of("ok", false, "message", msg));
            }
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", msg));
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
}
