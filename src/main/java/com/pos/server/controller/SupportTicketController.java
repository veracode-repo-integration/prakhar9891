package com.pos.server.controller;

import com.pos.server.model.SupportTicket;
import com.pos.server.model.User;
import com.pos.server.repository.SupportTicketRepository;
import com.pos.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/support/tickets")
@CrossOrigin(origins = "*")
public class SupportTicketController {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Value("${superadmin.phones:9876543210,9891260801}")
    private String superAdminPhones;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Unauthorized");
        }
        return userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        
        if (hasRole) return;

        if (superAdminPhones != null && username != null) {
            String cleanUser = username.replaceAll("\\D", "");
            boolean isPhoneMatch = java.util.Arrays.stream(superAdminPhones.split(","))
                    .map(p -> p.replaceAll("\\D", ""))
                    .anyMatch(p -> p.equals(cleanUser) || cleanUser.endsWith(p));
            if (isPhoneMatch) return;
        }
        throw new RuntimeException("Forbidden");
    }

    @PostMapping
    public ResponseEntity<?> createTicket(@RequestBody SupportTicket ticket) {
        try {
            User user = getAuthenticatedUser();
            ticket.setUserId(user.getUsername());
            ticket.setUserName(user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : ""));
            ticket.setRestaurantId(user.getRestaurantId());
            // restaurantName should be sent by frontend or looked up
            SupportTicket saved = supportTicketRepository.save(ticket);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyTickets() {
        try {
            User user = getAuthenticatedUser();
            List<SupportTicket> tickets = supportTicketRepository.findAllByRestaurantId(user.getRestaurantId());
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllTickets() {
        try {
            validateSuperAdmin();
            return ResponseEntity.ok(supportTicketRepository.findAll());
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            validateSuperAdmin();
            Optional<SupportTicket> ticketOpt = supportTicketRepository.findById(id);
            if (ticketOpt.isPresent()) {
                SupportTicket ticket = ticketOpt.get();
                if (body.containsKey("status")) {
                    ticket.setStatus(body.get("status"));
                }
                return ResponseEntity.ok(supportTicketRepository.save(ticket));
            }
            return ResponseEntity.status(404).body(Map.of("error", "Ticket not found"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
