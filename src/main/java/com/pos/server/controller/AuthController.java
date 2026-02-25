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
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private com.pos.server.repository.RestaurantRepository restaurantRepository;

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
            String token = jwtUtils.generateToken(user.getUsername());

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
            String token = jwtUtils.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", createUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }
    }

    @PostMapping("/otp-login")
    public ResponseEntity<?> otpLogin(@RequestBody Map<String, String> request) {
        try {
            String phone = request.get("phone");
            String restaurantId = request.get("restaurantId");
            String name = request.get("name");
            User user = authService.loginByPhone(phone, restaurantId, name);
            String token = jwtUtils.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", createUserResponse(user));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            String username = auth.getName();

            User user = authService.findByUsername(username);

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
