package com.pos.server.repository;

import com.pos.server.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Restaurant Table management
 */
@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, String> {
    List<RestaurantTable> findByArea(String area);

    List<RestaurantTable> findByStatus(String status);

    List<RestaurantTable> findAllByRestaurantId(String restaurantId);
}
