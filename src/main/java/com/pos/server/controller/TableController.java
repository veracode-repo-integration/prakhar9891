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
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @GetMapping("/tables")
    public ResponseEntity<List<RestaurantTable>> getAllTables(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        return ResponseEntity.ok(tableService.getAllTables(restaurantId));
    }

    @GetMapping("/tables/area/{area}")
    public ResponseEntity<List<RestaurantTable>> getTablesByArea(@PathVariable String area) {
        return ResponseEntity.ok(tableService.getTablesByArea(area));
    }

    @GetMapping("/tables/status/{status}")
    public ResponseEntity<List<RestaurantTable>> getTablesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(tableService.getTablesByStatus(status));
    }

    @PostMapping("/tables")
    public ResponseEntity<?> createTable(@RequestBody RestaurantTable table,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            if (restaurantId != null)
                table.setRestaurantId(restaurantId.toLowerCase().trim());
            RestaurantTable saved = tableService.createTable(table);
            return ResponseEntity.ok(Map.of("ok", true, "table", saved));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/tables/{id}")
    public ResponseEntity<?> updateTable(@PathVariable String id, @RequestBody Map<String, Object> updates,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        return tableService.updateTable(id, updates, restaurantId)
                .map(table -> {
                    // Notify subscribers about table update (trigger sound alert)
                    org.springframework.security.core.Authentication authInfo = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    String actingUser = (authInfo != null) ? authInfo.getName() : "system";

                    messagingTemplate.convertAndSend("/topic/orders", Map.of(
                        "type", "table_update", 
                        "tableId", table.getId(),
                        "actingUser", actingUser,
                        "table", table
                    ));
                    return ResponseEntity.ok(Map.of("ok", true, "table", table));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/tables/{id}/status")
    public ResponseEntity<?> updateTableStatus(@PathVariable String id, @RequestBody Map<String, Object> statusUpdate,
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        String status = (String) statusUpdate.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Status is required"));
        }
        return tableService.updateTableStatus(id, status, restaurantId)
                .map(table -> {
                    // Notify subscribers (triggers sound alert)
                    org.springframework.security.core.Authentication authInfo = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    String actingUser = (authInfo != null) ? authInfo.getName() : "system";

                    messagingTemplate.convertAndSend("/topic/orders", Map.of(
                        "type", "table_status_change", 
                        "tableId", table.getId(),
                        "actingUser", actingUser,
                        "table", table
                    ));
                    return ResponseEntity.ok(Map.of("ok", true, "table", table));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/tables/{id}")
    public ResponseEntity<?> deleteTable(@PathVariable String id) {
        if (tableService.deleteTable(id)) {
            return ResponseEntity.ok(Map.of("ok", true, "message", "Table deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }
}
