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

    @Autowired
    private com.pos.server.repository.UserRepository userRepository;

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

            // 1. Get managed restaurants from DB
            List<Restaurant> restaurants = restaurantRepository.findAll();
            java.util.Set<String> existingIds = restaurants.stream()
                    .map(Restaurant::getId)
                    .collect(java.util.stream.Collectors.toSet());

            // 2. Discover "Implicit" restaurants from Users table
            java.util.List<User> allUsers = userRepository.findAll();
            java.util.Map<String, List<User>> usersByRestaurant = allUsers.stream()
                    .filter(u -> u.getRestaurantId() != null && !u.getRestaurantId().trim().isEmpty())
                    .collect(java.util.stream.Collectors.groupingBy(User::getRestaurantId));

            for (String rid : usersByRestaurant.keySet()) {
                if (!existingIds.contains(rid)) {
                    // Create a transient placeholder for display
                    Restaurant placeholder = new Restaurant();
                    placeholder.setId(rid);
                    placeholder.setName(rid.substring(0, 1).toUpperCase() + rid.substring(1) + " (Inferred)");
                    placeholder.setStatus("ACTIVE");
                    placeholder.setPlanName("TRIAL");

                    // Try to pick a name from one of the users
                    List<User> rUsers = usersByRestaurant.get(rid);
                    Optional<User> admin = rUsers.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                            .findFirst();
                    if (admin.isPresent()) {
                        placeholder.setPhone(admin.get().getContact());
                        placeholder.setEmail(admin.get().getEmail());
                        if (admin.get().getFirstName() != null) {
                            placeholder.setName(admin.get().getFirstName() + "'s Shop");
                        }
                    }

                    restaurants.add(placeholder);
                }
            }

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

            // Discovery Fallback
            List<User> users = userRepository.findAllByRestaurantId(id);
            if (!users.isEmpty()) {
                Restaurant placeholder = new Restaurant();
                placeholder.setId(id);
                placeholder.setName(id + " (Found in Users)");
                return ResponseEntity.ok(placeholder);
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
            Restaurant r;

            if (restaurantOpt.isPresent()) {
                r = restaurantOpt.get();
            } else {
                // Auto-create restaurant entry if it exists in system but not in table
                r = new Restaurant();
                r.setId(id);
            }

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
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
