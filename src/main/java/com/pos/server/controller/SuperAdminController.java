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
            List<Restaurant> restaurants = restaurantRepository.findAll();
            java.util.Set<String> existingIds = restaurants.stream()
                    .map(Restaurant::getId)
                    .collect(java.util.stream.Collectors.toSet());

            java.util.List<User> allUsers = userRepository.findAll();
            java.util.Map<String, List<User>> usersByRestaurant = allUsers.stream()
                    .filter(u -> u.getRestaurantId() != null && !u.getRestaurantId().trim().isEmpty())
                    .collect(java.util.stream.Collectors.groupingBy(User::getRestaurantId));

            for (String rid : usersByRestaurant.keySet()) {
                if (!existingIds.contains(rid)) {
                    Restaurant placeholder = new Restaurant();
                    placeholder.setId(rid);
                    // Use admin user's name if available, else just the raw ID
                    String displayName = rid;
                    List<User> rUsers = usersByRestaurant.get(rid);
                    Optional<User> admin = rUsers.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                            .findFirst();
                    if (admin.isPresent() && admin.get().getFirstName() != null
                            && !admin.get().getFirstName().isEmpty()) {
                        displayName = admin.get().getFirstName() + "'s Store";
                    }
                    placeholder.setName(displayName);
                    placeholder.setStatus("ACTIVE");
                    placeholder.setPlanName("TRIAL");

                    if (admin.isPresent()) {
                        placeholder.setPhone(admin.get().getContact());
                        placeholder.setEmail(admin.get().getEmail());
                    }
                    restaurants.add(placeholder);
                }
            }
            return ResponseEntity.ok(restaurants);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            validateSuperAdmin();
            List<Restaurant> all = restaurantRepository.findAll();
            List<User> allUsers = userRepository.findAll();

            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalRestaurants", all.size());
            stats.put("activeRestaurants", all.stream().filter(r -> "ACTIVE".equals(r.getStatus())).count());
            stats.put("totalUsers", allUsers.size());

            // Estimated Revenue
            long monthlyRevenue = all.stream().mapToLong(r -> {
                if ("PRO".equalsIgnoreCase(r.getPlanName()))
                    return 2999;
                if ("BASIC".equalsIgnoreCase(r.getPlanName()))
                    return 999;
                return 0;
            }).sum();

            stats.put("monthlyRevenue", monthlyRevenue);

            // Plan Breakup
            Map<String, Long> plans = all.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            r -> r.getPlanName() != null ? r.getPlanName() : "FREE",
                            java.util.stream.Collectors.counting()));
            stats.put("planBreakup", plans);

            return ResponseEntity.ok(stats);
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

            // Discovery Fallback - use admin's name if available
            List<User> users = userRepository.findAllByRestaurantId(id);
            if (!users.isEmpty()) {
                Restaurant placeholder = new Restaurant();
                placeholder.setId(id);
                String displayName = id;
                Optional<User> admin = users.stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).findFirst();
                if (admin.isPresent() && admin.get().getFirstName() != null && !admin.get().getFirstName().isEmpty()) {
                    displayName = admin.get().getFirstName() + "'s Store";
                    placeholder.setPhone(admin.get().getContact());
                    placeholder.setEmail(admin.get().getEmail());
                }
                placeholder.setName(displayName);
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

    @GetMapping("/restaurants/{id}/users")
    public ResponseEntity<?> getUsersOfRestaurant(@PathVariable String id) {
        try {
            validateSuperAdmin();
            List<User> users = userRepository.findAllByRestaurantId(id);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/restaurants/{id}")
    public ResponseEntity<?> deleteRestaurant(@PathVariable String id) {
        try {
            validateSuperAdmin();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
        try {
            // Delete from restaurant table if it exists
            if (restaurantRepository.existsById(id)) {
                restaurantRepository.deleteById(id);
            }
            // Also remove all users belonging to this restaurant (handles inferred stores)
            List<com.pos.server.model.User> users = userRepository.findAllByRestaurantId(id);
            for (com.pos.server.model.User u : users) {
                userRepository.delete(u);
            }
            return ResponseEntity.ok(Map.of("ok", true,
                    "message", "Store " + id + " and " + users.size() + " user(s) removed."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", "Delete failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<?> deleteUser(@PathVariable String username) {
        try {
            validateSuperAdmin();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
        try {
            Optional<com.pos.server.model.User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                userRepository.delete(userOpt.get());
                return ResponseEntity.ok(Map.of("ok", true, "message", "User " + username + " deleted"));
            } else {
                return ResponseEntity.ok(Map.of("ok", true, "message", "User not found, nothing to delete"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", "Delete failed: " + e.getMessage()));
        }
    }
}
