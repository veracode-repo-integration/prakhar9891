package com.pos.server.controller;

import com.pos.server.dto.UserResponse;
import com.pos.server.model.User;
import com.pos.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.pos.server.service.AuthService authService;

    // List all users - ADMIN OR SUPER_ADMIN ONLY
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            List<User> users = userRepository.findAllByRestaurantId(rid);
            return ResponseEntity.ok(users.stream()
                .filter(u -> !"GUEST".equalsIgnoreCase(u.getRole()))
                .filter(u -> u.getUsername() != null && !u.getUsername().startsWith("deleted_"))
                .map(u -> new UserResponse(
                        u.getDbId(),
                        u.getUsername(),
                        (u.getFirstName() != null ? u.getFirstName() : u.getUsername()),
                        u.getEmail(),
                        u.getContact(),
                        u.getRole(),
                        u.getRestaurantId(),
                        "OPEN"))
                .collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }

    // Update user role - ADMIN ONLY
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable String id, @RequestBody Map<String, String> payload) {
        String newRole = payload.get("role");
        if (newRole == null || (!newRole.equals("admin") && !newRole.equals("staff"))) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Invalid role"));
        }

        try {
            Long userId = Long.parseLong(id);
            return userRepository.findById(userId).map(user -> {
                user.setRole(newRole);
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("ok", true));
            }).orElse(ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Invalid ID format"));
        }
    }
}
