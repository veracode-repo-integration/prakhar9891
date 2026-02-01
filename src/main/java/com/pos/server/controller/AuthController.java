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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody com.pos.server.dto.RegisterRequest request) {
        try {
            User user = authService.register(request);
            String token = jwtUtils.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("token", token);
            response.put("user", new com.pos.server.dto.UserResponse(
                    user.getDbId(),
                    user.getUsername(),
                    (user.getFirstName() != null ? user.getFirstName() : user.getUsername()),
                    user.getEmail(),
                    user.getContact(),
                    user.getRole()));
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
            response.put("user", new com.pos.server.dto.UserResponse(
                    user.getDbId(),
                    user.getUsername(),
                    (user.getFirstName() != null ? user.getFirstName() : user.getUsername()),
                    user.getEmail(),
                    user.getContact(),
                    user.getRole()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
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
