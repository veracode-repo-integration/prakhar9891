package com.pos.server.service;

import com.pos.server.model.User;
import com.pos.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.pos.server.repository.CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${superadmin.phones:9876543210,9891260801}")
    private String superAdminPhones;

    private User assignSuperAdminIfApplicable(User user, String cleanPhone) {
        if (superAdminPhones != null && !superAdminPhones.isEmpty()) {
            boolean isSuperAdmin = java.util.Arrays.stream(superAdminPhones.split(","))
                    .map(p -> p.replaceAll("\\D", ""))
                    .anyMatch(p -> p.equals(cleanPhone) || cleanPhone.endsWith(p));
            if (isSuperAdmin && !"SUPER_ADMIN".equals(user.getRole())) {
                user.setRole("SUPER_ADMIN");
                return userRepository.save(user); // Force role upgrade and save
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

        return userRepository.save(user);
    }

    public User login(String identifier, String providedPassword) {
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty())
            userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isEmpty())
            userOpt = userRepository.findByContact(identifier);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(providedPassword, user.getPasswordHash())) {
                return user;
            }
        }
        throw new RuntimeException("Invalid credentials");
    }

    public void deleteUser(String username) {
        User user = findByUsername(username);
        userRepository.delete(user);
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

    public User loginByPhone(String phone, String restaurantId, String providedName) {
        // 1. Clean phone number for normalized matching
        String cleanPhone = phone.replaceAll("\\D", "");

        // 2. Try various matching strategies
        Optional<User> userOpt = userRepository.findByContact(phone);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getContact() != null && u.getContact().replaceAll("\\D", "").endsWith(
                            cleanPhone.length() >= 10 ? cleanPhone.substring(cleanPhone.length() - 10) : cleanPhone))
                    .findFirst();
        }

        if (userOpt.isPresent()) {
            return assignSuperAdminIfApplicable(userOpt.get(), cleanPhone);
        }

        // 3. DINE-IN GUEST BYPASS: Only create guest record if restaurantId is present
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

        if (userOpt.isPresent()) {
            return assignSuperAdminIfApplicable(userOpt.get(), cleanPhone);
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

        return assignSuperAdminIfApplicable(userRepository.save(newUser), cleanPhone);
    }
}
