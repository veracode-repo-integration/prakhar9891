package com.pos.server.service;

import com.pos.server.model.User;
import com.pos.server.model.Restaurant;
import com.pos.server.repository.UserRepository;
import com.pos.server.repository.RestaurantRepository;
import com.pos.server.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.pos.server.security.FirebaseService firebaseService;

    @org.springframework.beans.factory.annotation.Value("${superadmin.phones:9891260801}")
    private String superAdminPhones;

    private User assignSuperAdminIfApplicable(User user, String cleanPhone) {
        if (superAdminPhones != null && !superAdminPhones.isEmpty()) {
            boolean isSuperAdmin = java.util.Arrays.stream(superAdminPhones.split(","))
                    .map(p -> p.replaceAll("\\D", ""))
                    .filter(p -> p.length() >= 10)
                    .anyMatch(p -> p.equals(cleanPhone) || (cleanPhone.length() >= 10 && cleanPhone.endsWith(p)));
            if (isSuperAdmin) {
                user.setRole("SUPER_ADMIN");
                // DO NOT save to repository. This role is transient for the session.
                return user;
            }
        }
        return user;
    }

    public User register(com.pos.server.dto.RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("User exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setContact(request.getContact());
        user.setRole(request.getRole() != null ? request.getRole() : "STAFF");
        user.setRestaurantId(request.getRestaurantId() != null ? request.getRestaurantId().toLowerCase().trim() : null);

        User saved = userRepository.save(user);
        return saved;
    }

    public User login(String identifier, String providedPassword) {
        // Hardcoded Super Admin Bypass (Username/Password)
        if ("superadmin".equals(identifier) && "superadmin@941219".equals(providedPassword)) {
            System.out.println("[AuthService] Super Admin Password Bypass triggered for: " + identifier);
            User sa = userRepository.findByUsername("superadmin").orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername("superadmin");
                newUser.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                newUser.setRole("ADMIN");
                newUser.setFirstName("Super");
                newUser.setLastName("Admin");
                newUser.setContact("9891260801");
                return userRepository.save(newUser);
            });
            sa.setRole("SUPER_ADMIN");
            return sa;
        }

        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty())
            userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty())
            userOpt = userRepository.findByContact(identifier);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(providedPassword, user.getPasswordHash())) {
                // Ensure Super Admin status is reassigned even on password login
                user = assignSuperAdminIfApplicable(user, user.getContact() != null ? user.getContact().replaceAll("\\D", "") : "");
                checkSubscription(user);
                return user;
            }
        }
        throw new RuntimeException("Invalid credentials");
    }

    public void deleteUser(String username) {
        User user = findByUsername(username);
        // Robust Deletion: Instead of physical delete which breaks foreign keys (orders, etc.)
        // We de-identify the user to "free up" the username and contact
        String timestamp = String.valueOf(System.currentTimeMillis());
        user.setUsername("deleted_" + user.getUsername() + "_" + timestamp);
        user.setContact("deleted_" + timestamp);
        user.setEmail(null);
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean checkUserExistsByPhone(String phone) {
        String cleanPhone = phone.replaceAll("\\D", "");
        return userRepository.findAll().stream()
                .anyMatch(u -> {
                    if (u.getContact() == null)
                        return false;
                    String c = u.getContact().replaceAll("\\D", "");
                    return c.endsWith(
                            cleanPhone.length() >= 10 ? cleanPhone.substring(cleanPhone.length() - 10) : cleanPhone);
                });
    }

    public User loginByPhone(String idToken, String restaurantId, String providedName) {
        String phone;
        try {
            // 1. Mandatory Backend Verification (with Super Admin Bypass)
            if ("super-admin-bypass".equals(idToken)) {
                phone = "+919891260801";
                System.out.println("[AuthService] Super Admin Bypass used for: " + phone);
            } else {
                phone = firebaseService.verifyIdToken(idToken);
                System.out.println("[AuthService] Verified ID Token for: " + phone);
            }
        } catch (Exception e) {
            throw new RuntimeException("Authentication Failed: " + e.getMessage());
        }

        // 2. Clean phone number for normalized matching
        String cleanPhone = phone.replaceAll("\\D", "");

        // 3. Try various matching strategies
        Optional<User> userOpt = userRepository.findByContact(phone);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getContact() != null && u.getContact().replaceAll("\\D", "").endsWith(
                            cleanPhone.length() >= 10 ? cleanPhone.substring(cleanPhone.length() - 10) : cleanPhone))
                    .findFirst();
        }

        if (userOpt.isPresent()) {
            User user = assignSuperAdminIfApplicable(userOpt.get(), cleanPhone);
            checkSubscription(user);
            return user;
        }

        // 4. DINE-IN GUEST BYPASS: Only create guest record if restaurantId is present
        // (implies Dine-In context)
        if (restaurantId != null && !restaurantId.trim().isEmpty()) {
            System.out.println("[AuthService] No user for " + phone + ". Creating Guest record for Dine-In flow.");
            User guest = new User();
            guest.setUsername("customer_" + cleanPhone);
            guest.setContact(phone);
            guest.setFirstName(providedName != null && !providedName.isEmpty() ? providedName : "Guest");
            guest.setRole("GUEST");
            guest.setRestaurantId(restaurantId);
            guest.setPasswordHash(passwordEncoder.encode("GUEST_" + java.util.UUID.randomUUID().toString()));

            User savedUser = userRepository.save(guest);
            User finalUser = assignSuperAdminIfApplicable(savedUser, cleanPhone);
            checkSubscription(finalUser);
            return finalUser;

            // Also save to Customers table so they appear in the Customers list for the
            // shopkeeper
            try {
                if (customerRepository.findFirstByMobileAndRestaurantId(phone, restaurantId).isEmpty()) {
                    com.pos.server.model.Customer customer = new com.pos.server.model.Customer();
                    customer.setId("cust_" + cleanPhone + "_" + restaurantId);
                    customer.setName(providedName != null && !providedName.isEmpty() ? providedName : "Guest " + phone);
                    customer.setMobile(phone);
                    customer.setType("New");
                    customer.setRestaurantId(restaurantId);
                    customer.setJoinedAt(java.time.OffsetDateTime.now().toString());
                    customerRepository.save(customer);
                }
            } catch (Exception e) {
                System.err.println("[AuthService] Failed to create customer record: " + e.getMessage());
            }

            return savedUser;
        }

        // Auto-Register New User
        User newUser = new User();
        newUser.setUsername(phone);
        newUser.setContact(phone);
        // Default to ADMIN so they can setup their shop
        newUser.setRole("ADMIN");
        // Use phone as default Restaurant ID (Unique & Memorable)
        newUser.setRestaurantId(phone);
        // Set a random password since they used OTP
        newUser.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

        User savedUser = userRepository.save(newUser);
        User finalUser = assignSuperAdminIfApplicable(savedUser, cleanPhone);
        checkSubscription(finalUser);
        return finalUser;
    }

    public void checkSubscription(User user) {
        // Robust Multi-Layered Super Admin Bypass
        if (user != null && user.getRole() != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRole())) {
            return; // Super Admin always has access
        }

        // Emergency fallback check against hardcoded Super Admin phones
        if (user != null && user.getContact() != null && !user.getContact().isEmpty()) {
            String cleanUserPhone = user.getContact().replaceAll("\\D", "");
            boolean isSaByPhone = java.util.Arrays.stream(superAdminPhones.split(","))
                    .map(p -> p.replaceAll("\\D", ""))
                    .filter(p -> p.length() >= 10)
                    .anyMatch(p -> p.equals(cleanUserPhone) || (cleanUserPhone.length() >= 10 && cleanUserPhone.endsWith(p)));
            if (isSaByPhone) {
                user.setRole("SUPER_ADMIN"); // Safe guard role for the session
                return;
            }
        }

        // --- GUEST BYPASS ---
        // Allow Dine-In Guests to see the menu even if restaurant is pending setup/suspended
        // so testing isn't blocked.
        if (user != null && "GUEST".equalsIgnoreCase(user.getRole())) {
            return;
        }

        if (user.getRestaurantId() != null && isRestaurantSuspended(user.getRestaurantId())) {
            // Final Super Admin check before throwing
            if (user.getContact() != null) {
                String cleanUserPhone = user.getContact().replaceAll("\\D", "");
                boolean isSaByPhone = java.util.Arrays.stream(superAdminPhones.split(","))
                        .map(p -> p.replaceAll("\\D", ""))
                        .filter(p -> p.length() >= 10)
                        .anyMatch(p -> p.equals(cleanUserPhone) || (cleanUserPhone.length() >= 10 && cleanUserPhone.endsWith(p)));
                if (isSaByPhone) return;
            }
            throw new RuntimeException("Forbidden: Account is suspended");
        }
    }

    public boolean isRestaurantSuspended(String restaurantId) {
        if (restaurantId == null)
            return false;
        Optional<Restaurant> restOpt = restaurantRepository.findById(restaurantId.toLowerCase());
        if (restOpt.isPresent()) {
            Restaurant r = restOpt.get();
            return "SUSPENDED".equalsIgnoreCase(r.getStatus());
        }
        return false;
    }

    /**
     * Centralized tenant isolation check.
     * Ensures users can only access their own restaurant's data.
     * SUPER_ADMIN can bypass.
     */
    public String validateRestaurantAccess(String headerId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("Unauthorized: Session not found");
        }

        String username = auth.getName();
        User user = findByUsername(username);

        // Robust Super Admin check: Check both DB role AND current Authentication authorities
        boolean isSuperAdmin = "SUPER_ADMIN".equalsIgnoreCase(user.getRole()) ||
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (isSuperAdmin) {
            // Super Admin can act on any restaurant via header, or their own (if assigned)
            String targetRid = (headerId != null && !headerId.trim().isEmpty()) ? headerId.toLowerCase().trim()
                    : user.getRestaurantId();
            return targetRid;
        }

        // Strict Tenant Isolation for everyone else
        String userRid = user.getRestaurantId();
        if (userRid == null || userRid.trim().isEmpty()) {
            throw new RuntimeException("Forbidden: User has no assigned restaurant");
        }

        return userRid.toLowerCase().trim();
    }
}
