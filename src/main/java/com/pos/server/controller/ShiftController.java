package com.pos.server.controller;

import com.pos.server.model.Shift;
import com.pos.server.repository.ShiftRepository;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private AuthService authService;

    /**
     * Get the active open shift for the restaurant.
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveShift(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            Optional<Shift> activeOpt = shiftRepository.findFirstByRestaurantIdAndStatusIgnoreCase(rid, "OPEN");
            
            if (activeOpt.isPresent()) {
                return ResponseEntity.ok(activeOpt.get());
            } else {
                return ResponseEntity.ok(Map.of("status", "CLOSED"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Open a new shift with a cash drawer opening balance.
     */
    @PostMapping("/open")
    public ResponseEntity<?> openShift(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            
            // Check if there is already an open shift
            Optional<Shift> activeOpt = shiftRepository.findFirstByRestaurantIdAndStatusIgnoreCase(rid, "OPEN");
            if (activeOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "An active shift is already open for this restaurant."));
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : "system";

            double openingBalance = Double.parseDouble(body.getOrDefault("openingBalance", "0.0").toString());
            String notes = (String) body.get("notes");

            Shift shift = new Shift();
            shift.setId(UUID.randomUUID().toString());
            shift.setRestaurantId(rid);
            shift.setUserId(username);
            shift.setOpenTime(System.currentTimeMillis());
            shift.setOpeningBalance(openingBalance);
            shift.setExpectedCash(openingBalance);
            shift.setNotes(notes);
            shift.setStatus("OPEN");

            Shift saved = shiftRepository.save(shift);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Close the active shift, record actual cash in drawer, and calculate discrepancies.
     */
    @PostMapping("/close")
    public ResponseEntity<?> closeShift(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            
            Optional<Shift> activeOpt = shiftRepository.findFirstByRestaurantIdAndStatusIgnoreCase(rid, "OPEN");
            if (!activeOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No active shift found to close."));
            }

            Shift shift = activeOpt.get();
            double actualCash = Double.parseDouble(body.getOrDefault("actualCash", "0.0").toString());
            String notes = (String) body.get("notes");

            shift.setCloseTime(System.currentTimeMillis());
            shift.setActualCash(actualCash);
            shift.setStatus("CLOSED");
            if (notes != null) {
                shift.setNotes((shift.getNotes() != null ? shift.getNotes() + "\n" : "") + "Closing Notes: " + notes);
            }

            Shift saved = shiftRepository.save(shift);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
