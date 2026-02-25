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

    private void validateSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Unauthorized");
        }

        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!isSuperAdmin) {
            throw new RuntimeException("Forbidden: Only Super Admins can access this endpoint");
        }
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
