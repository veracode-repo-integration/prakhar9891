package com.pos.server.repository;

import com.pos.server.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Restaurant Table management
 */
@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, String> {
    List<RestaurantTable> findByArea(String area);

    List<RestaurantTable> findByStatus(String status);

    List<RestaurantTable> findAllByRestaurantId(String restaurantId);

    Optional<RestaurantTable> findByIdAndRestaurantId(String id, String restaurantId);

    // Fix for NonUniqueResultException if dirty data exists
    List<RestaurantTable> findAllByIdAndRestaurantId(String id, String restaurantId);

    Optional<RestaurantTable> findByNameAndRestaurantId(String name, String restaurantId);
}
