package com.pos.server.service;

import com.pos.server.model.User;
import com.pos.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegister() {
        com.pos.server.dto.RegisterRequest request = new com.pos.server.dto.RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("plainpass");

        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hashedpass");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainpass")).thenReturn("hashedpass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User registered = authService.register(request);
        assertNotNull(registered);
    }

    @Test
    void testLoginSuccess() {
        User user = new User();
        user.setUsername("admin");
        user.setPasswordHash("hashedpass");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainpass", "hashedpass")).thenReturn(true);

        User loggedIn = authService.login("admin", "plainpass");
        assertNotNull(loggedIn);
    }
}
