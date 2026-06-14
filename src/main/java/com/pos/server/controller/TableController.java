package com.pos.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.server.model.RestaurantTable;
import com.pos.server.repository.RestaurantTableRepository;
import com.pos.server.service.TableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Restaurant Table management
 * Handles table CRUD operations and status updates
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class TableController {

    @Autowired
    private TableService tableService;

    @Autowired
    private com.pos.server.service.AuthService authService;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @GetMapping("/tables")
    public ResponseEntity<?> getAllTables(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(tableService.getAllTables(rid));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage() != null ? e.getMessage() : "Unknown Error"));
        }
    }

    @GetMapping("/tables/area/{area}")
    public ResponseEntity<?> getTablesByArea(
            @PathVariable String area,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(tableService.getTablesByArea(area, rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }

    @GetMapping("/tables/status/{status}")
    public ResponseEntity<?> getTablesByStatus(
            @PathVariable String status,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            return ResponseEntity.ok(tableService.getTablesByStatus(status, rid));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new java.util.ArrayList<>());
        }
    }

    @PostMapping("/tables")
    public ResponseEntity<?> createTable(@RequestBody RestaurantTable table,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            table.setRestaurantId(rid);
            RestaurantTable saved = tableService.createTable(table);
            return ResponseEntity.ok(Map.of("ok", true, "table", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/tables/{id}")
    public ResponseEntity<?> updateTable(@PathVariable String id, @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            authService.validateTableAccess(id); // Enforce table containment for guests
            return tableService.updateTable(id, updates, rid)
                    .map(table -> {
                        // Notify subscribers about table update (trigger sound alert)
                        org.springframework.security.core.Authentication authInfo = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                        String actingUser = (authInfo != null) ? authInfo.getName() : "system";

                        messagingTemplate.convertAndSend("/topic/orders", Map.of(
                                "type", "table_update",
                                "tableId", table.getId(),
                                "actingUser", actingUser,
                                "table", table));
                        return ResponseEntity.ok(Map.of("ok", true, "table", table));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/tables/{id}/status")
    public ResponseEntity<?> updateTableStatus(@PathVariable String id, @RequestBody Map<String, Object> statusUpdate,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            authService.validateTableAccess(id); // Enforce table containment for guests
            String status = (String) statusUpdate.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Status is required"));
            }
            return tableService.updateTableStatus(id, status, rid)
                    .map(table -> {
                        // Notify subscribers (triggers sound alert)
                        org.springframework.security.core.Authentication authInfo = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                        String actingUser = (authInfo != null) ? authInfo.getName() : "system";

                        messagingTemplate.convertAndSend("/topic/orders", Map.of(
                                "type", "table_status_change",
                                "tableId", table.getId(),
                                "actingUser", actingUser,
                                "table", table));
                        return ResponseEntity.ok(Map.of("ok", true, "table", table));
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/tables/{id}")
    public ResponseEntity<?> deleteTable(
            @PathVariable String id,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            if (tableService.deleteTable(id, rid)) {
                return ResponseEntity.ok(Map.of("ok", true, "message", "Table deleted successfully"));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

}
