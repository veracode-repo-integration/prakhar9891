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

    // List all users - ADMIN ONLY
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        
        // Belt and Suspenders: Manual Check + @PreAuthorize
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
             return ResponseEntity.status(403).body(Map.of("message", "Access Denied"));
        }

        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(u -> new UserResponse(
                        u.getDbId(),
                        u.getUsername(),
                        (u.getFirstName() != null ? u.getFirstName() : u.getUsername()),
                        u.getEmail(),
                        u.getContact(),
                        u.getRole()))
                .collect(Collectors.toList()));
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
