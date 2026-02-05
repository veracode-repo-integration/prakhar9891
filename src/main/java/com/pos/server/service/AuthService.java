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
    private PasswordEncoder passwordEncoder;

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

    public User loginByPhone(String phone, String restaurantId) {
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
            return userOpt.get();
        }

        // 3. DINE-IN GUEST BYPASS: If no user found, auto-register as GUEST
        System.out.println("[AuthService] No user for " + phone + ". Creating Guest record for Dine-In flow.");
        User guest = new User();
        // Use sanitized phone as username
        guest.setUsername("customer_" + cleanPhone);
        guest.setContact(phone);
        guest.setFirstName("Guest");
        guest.setRole("GUEST");
        guest.setRestaurantId(restaurantId != null ? restaurantId : null);
        // Random secure password hash that won't be used
        guest.setPasswordHash(passwordEncoder.encode("GUEST_" + java.util.UUID.randomUUID().toString()));

        return userRepository.save(guest);
    }
}
