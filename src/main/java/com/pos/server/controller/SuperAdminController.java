package com.pos.server.controller;

import com.pos.server.model.Restaurant;
import com.pos.server.model.User;
import com.pos.server.repository.RestaurantRepository;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/superadmin")
@CrossOrigin(origins = "*")
public class SuperAdminController {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private AuthService authService;

    @org.springframework.beans.factory.annotation.Value("${superadmin.phones:9876543210,9891260801}")
    private String superAdminPhones;

    private void validateSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Unauthorized: Session not found");
        }

        // 1. Check Authority (Transitional role from JWT)
        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (hasRole)
            return;

        // 2. Fallback: Check if the authenticated username (phone) is in the superadmin
        // list
        String username = auth.getName();
        if (superAdminPhones != null && username != null) {
            String cleanUser = username.replaceAll("\\D", "");
            boolean isPhoneMatch = java.util.Arrays.stream(superAdminPhones.split(","))
                    .map(p -> p.replaceAll("\\D", ""))
                    .anyMatch(p -> p.equals(cleanUser) || cleanUser.endsWith(p));

            if (isPhoneMatch)
                return;
        }

        throw new RuntimeException("Forbidden: Access denied for " + username);
    }

    @GetMapping("/restaurants")
    public ResponseEntity<?> getAllRestaurants() {
        try {
            validateSuperAdmin();
            List<Restaurant> restaurants = restaurantRepository.findAll();
            return ResponseEntity.ok(restaurants);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/restaurants/{id}")
    public ResponseEntity<?> getRestaurant(@PathVariable String id) {
        try {
            validateSuperAdmin();
            Optional<Restaurant> restaurantOpt = restaurantRepository.findById(id);
            if (restaurantOpt.isPresent()) {
                return ResponseEntity.ok(restaurantOpt.get());
            }
            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Restaurant not found"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/restaurants/{id}")
    public ResponseEntity<?> updateRestaurant(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        try {
            validateSuperAdmin();
            Optional<Restaurant> restaurantOpt = restaurantRepository.findById(id);

            if (restaurantOpt.isPresent()) {
                Restaurant r = restaurantOpt.get();

                if (updates.containsKey("name"))
                    r.setName((String) updates.get("name"));
                if (updates.containsKey("status"))
                    r.setStatus((String) updates.get("status"));
                if (updates.containsKey("planName"))
                    r.setPlanName((String) updates.get("planName"));

                if (updates.containsKey("subscriptionEndDate")) {
                    Object dateObj = updates.get("subscriptionEndDate");
                    if (dateObj instanceof Number) {
                        r.setSubscriptionEndDate(((Number) dateObj).longValue());
                    } else if (dateObj == null) {
                        r.setSubscriptionEndDate(null);
                    }
                }

                if (updates.containsKey("allowedFeatures")) {
                    r.setAllowedFeatures((List<String>) updates.get("allowedFeatures"));
                }

                restaurantRepository.save(r);
                return ResponseEntity.ok(Map.of("ok", true, "restaurant", r));
            }

            return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Restaurant not found"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
