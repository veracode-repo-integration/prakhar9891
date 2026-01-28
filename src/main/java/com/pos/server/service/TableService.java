package com.pos.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.server.model.RestaurantTable;
import com.pos.server.repository.RestaurantTableRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class TableService {

    @Autowired
    private RestaurantTableRepository tableRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<RestaurantTable> getAllTables() {
        return tableRepository.findAll();
    }

    public List<RestaurantTable> getTablesByArea(String area) {
        return tableRepository.findByArea(area);
    }

    public List<RestaurantTable> getTablesByStatus(String status) {
        return tableRepository.findByStatus(status);
    }

    @Transactional
    public RestaurantTable createTable(RestaurantTable table) {
        if (table.getId() == null || table.getId().isEmpty()) {
            table.setId("t" + System.currentTimeMillis());
        }
        if (table.getStatus() == null) {
            table.setStatus("available");
        }
        log.info("Creating new table: {}", table.getId());
        return tableRepository.save(table);
    }

    @Transactional
    public Optional<RestaurantTable> updateTable(String id, Map<String, Object> updates) {
        return tableRepository.findById(id).map(table -> {
            try {
                if (updates.containsKey("name"))
                    table.setName((String) updates.get("name"));
                if (updates.containsKey("area"))
                    table.setArea((String) updates.get("area"));
                if (updates.containsKey("status"))
                    table.setStatus((String) updates.get("status"));
                if (updates.containsKey("capacity"))
                    table.setCapacity(((Number) updates.get("capacity")).intValue());

                // Handle currentOrder flexibly
                if (updates.containsKey("currentOrder")) {
                    Object orderObj = updates.get("currentOrder");
                    if (orderObj instanceof String) {
                        table.setCurrentOrder((String) orderObj);
                    } else if (orderObj != null) {
                        table.setCurrentOrder(mapper.writeValueAsString(orderObj));
                    } else {
                        table.setCurrentOrder(null);
                    }
                }

                if (updates.containsKey("guestCount"))
                    table.setGuestCount(((Number) updates.get("guestCount")).intValue());

                if (updates.containsKey("sessionStart")) {
                    Object sessionStart = updates.get("sessionStart");
                    if (sessionStart instanceof Number) {
                        table.setSessionStart(((Number) sessionStart).longValue());
                    } else {
                        table.setSessionStart(null);
                    }
                }

                if (updates.containsKey("kitchenStatus"))
                    table.setKitchenStatus((String) updates.get("kitchenStatus"));

                log.info("Updating table: {}", id);
                return tableRepository.save(table);
            } catch (Exception e) {
                log.error("Error updating table {}: ", id, e);
                throw new RuntimeException("Failed to update table: " + e.getMessage());
            }
        });
    }

    @Transactional
    public Optional<RestaurantTable> updateTableStatus(String id, String status) {
        return tableRepository.findById(id).map(table -> {
            table.setStatus(status);
            if ("available".equals(status)) {
                table.setCurrentOrder(null);
                table.setGuestCount(0);
                table.setSessionStart(null);
                table.setKitchenStatus(null);
            }
            log.info("Updated status of table {} to {}", id, status);
            return tableRepository.save(table);
        });
    }

    @Transactional
    public boolean deleteTable(String id) {
        if (tableRepository.existsById(id)) {
            tableRepository.deleteById(id);
            log.info("Deleted table: {}", id);
            return true;
        }
        return false;
    }
}
