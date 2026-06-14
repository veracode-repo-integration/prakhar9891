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

    public List<RestaurantTable> getAllTables(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return tableRepository.findAllByRestaurantIdIgnoreCase(restaurantId);
    }

    public List<RestaurantTable> getTablesByArea(String area, String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return tableRepository.findByArea(area);
        }
        return tableRepository.findByAreaAndRestaurantIdIgnoreCase(area, restaurantId);
    }

    public List<RestaurantTable> getTablesByStatus(String status, String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            return tableRepository.findByStatus(status);
        }
        return tableRepository.findByStatusAndRestaurantIdIgnoreCase(status, restaurantId);
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
    public Optional<RestaurantTable> updateTable(String id, Map<String, Object> updates, String restaurantId) {
        RestaurantTable table = null;

        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            Optional<RestaurantTable> opt = tableRepository.findById(id);
            if (opt.isPresent())
                table = opt.get();
        } else {
            // Robust lookup handling duplicates
            List<RestaurantTable> tables = tableRepository.findAllByIdAndRestaurantIdIgnoreCase(id, restaurantId);
            if (!tables.isEmpty()) {
                table = tables.get(0);
                if (tables.size() > 1) {
                    log.warn("Found {} duplicate tables for ID {} during update. Using first one.", tables.size(), id);
                }
            } else {
                // Try finding by Name if ID lookup fails (common if ID is actually a name)
                Optional<RestaurantTable> nameOpt = tableRepository.findByNameAndRestaurantIdIgnoreCase(id, restaurantId);
                if (nameOpt.isPresent())
                    table = nameOpt.get();
            }
        }

        return Optional.ofNullable(table).map(t -> {
            try {
                if (updates.containsKey("name"))
                    t.setName((String) updates.get("name"));
                if (updates.containsKey("area"))
                    t.setArea((String) updates.get("area"));
                if (updates.containsKey("status"))
                    t.setStatus((String) updates.get("status"));
                if (updates.containsKey("capacity"))
                    t.setCapacity(((Number) updates.get("capacity")).intValue());

                // Handle currentOrder flexibly
                if (updates.containsKey("currentOrder")) {
                    Object orderObj = updates.get("currentOrder");
                    if (orderObj instanceof String) {
                        t.setCurrentOrder((String) orderObj);
                    } else if (orderObj != null) {
                        t.setCurrentOrder(mapper.writeValueAsString(orderObj));
                    } else {
                        t.setCurrentOrder(null);
                    }
                }

                if (updates.containsKey("guestCount"))
                    t.setGuestCount(((Number) updates.get("guestCount")).intValue());

                if (updates.containsKey("sessionStart")) {
                    Object sessionStart = updates.get("sessionStart");
                    if (sessionStart instanceof Number) {
                        t.setSessionStart(((Number) sessionStart).longValue());
                    } else {
                        t.setSessionStart(null);
                    }
                }

                if (updates.containsKey("kitchenStatus"))
                    t.setKitchenStatus((String) updates.get("kitchenStatus"));

                if (updates.containsKey("customerName"))
                    t.setCustomerName((String) updates.get("customerName"));

                if (updates.containsKey("customerMobile"))
                    t.setCustomerMobile((String) updates.get("customerMobile"));

                log.info("Updating table: {}", t.getId());
                return tableRepository.save(t);
            } catch (Exception e) {
                log.error("Error updating table {}: ", id, e);
                throw new RuntimeException("Failed to update table: " + e.getMessage());
            }
        });
    }

    @Transactional
    public Optional<RestaurantTable> updateTableStatus(String id, String status, String restaurantId) {
        Optional<RestaurantTable> tableOpt = (restaurantId == null || restaurantId.trim().isEmpty())
                ? tableRepository.findById(id)
                : tableRepository.findByIdAndRestaurantIdIgnoreCase(id, restaurantId);

        return tableOpt.map(table -> {
            table.setStatus(status);
            if ("available".equals(status)) {
                table.setCurrentOrder(null);
                table.setGuestCount(0);
                table.setSessionStart(null);
                table.setKitchenStatus(null);
                table.setCustomerName(null);
                table.setCustomerMobile(null);
            }
            log.info("Updated status of table {} to {}", id, status);
            return tableRepository.save(table);
        });
    }

    @Transactional
    public boolean deleteTable(String id, String restaurantId) {
        Optional<RestaurantTable> tableOpt = tableRepository.findById(id);
        if (tableOpt.isPresent()) {
            RestaurantTable table = tableOpt.get();
            if (restaurantId == null || restaurantId.trim().isEmpty() || table.getRestaurantId().equalsIgnoreCase(restaurantId)) {
                tableRepository.delete(table);
                log.info("Deleted table: {}", id);
                return true;
            }
        }
        return false;
    }

}
