package com.pos.server.repository;

import com.pos.server.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findAllByRestaurantId(String restaurantId);

    java.util.Optional<Order> findByIdAndRestaurantId(String id, String restaurantId);

    org.springframework.data.domain.Page<Order> findAllByRestaurantId(String restaurantId,
            org.springframework.data.domain.Pageable pageable);
}
