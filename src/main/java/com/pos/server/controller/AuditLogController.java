package com.pos.server.controller;

import com.pos.server.model.AuditLog;
import com.pos.server.repository.AuditLogRepository;
import com.pos.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<?> logSecurityEvent(
            @RequestBody AuditLog logRequest,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String headerId) {
        try {
            String restaurantId = authService.validateRestaurantAccess(headerId);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null) ? auth.getName() : "anonymous";

            AuditLog log = new AuditLog();
            log.setRestaurantId(restaurantId);
            log.setAction(logRequest.getAction() != null ? logRequest.getAction().toUpperCase() : "GENERAL_SECURITY_EVENT");
            log.setPerformedBy(logRequest.getPerformedBy() != null ? logRequest.getPerformedBy() : username);
            log.setManagerAuthorized(Boolean.TRUE.equals(logRequest.getManagerAuthorized()));
            log.setReason(logRequest.getReason());
            log.setDetailsJson(logRequest.getDetailsJson());
            log.setCreatedAt(LocalDateTime.now());

            AuditLog saved = auditLogRepository.save(log);
            return ResponseEntity.ok(Map.of("ok", true, "logId", saved.getId(), "message", "Audit log recorded"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String headerId) {
        try {
            String restaurantId = authService.validateRestaurantAccess(headerId);
            Page<AuditLog> logs = auditLogRepository.findByRestaurantIdOrderByCreatedAtDesc(
                    restaurantId, PageRequest.of(page, Math.min(size, 100)));
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }
}
