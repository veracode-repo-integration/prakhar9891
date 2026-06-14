package com.pos.server.repository;

import com.pos.server.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, String> {
    Optional<Shift> findFirstByRestaurantIdAndUserIdAndStatusIgnoreCase(String restaurantId, String userId, String status);
    Optional<Shift> findFirstByRestaurantIdAndStatusIgnoreCase(String restaurantId, String status);
    java.util.List<Shift> findAllByRestaurantId(String restaurantId);
}
