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

    public User loginByPhone(String phone) {
        // Try finding by exact match or with +91 etc.
        Optional<User> userOpt = userRepository.findByContact(phone);
        if (userOpt.isEmpty() && phone.startsWith("+91")) {
            userOpt = userRepository.findByContact(phone.substring(3)); // Try without +91
        }

        if (userOpt.isPresent()) {
            return userOpt.get();
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

        return userRepository.save(newUser);
    }
}
