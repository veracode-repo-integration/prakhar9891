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
        user.setRole(request.getRole() != null ? request.getRole() : "staff"); // Default to staff

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
}
