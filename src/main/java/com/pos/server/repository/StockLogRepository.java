package com.pos.server.repository;

import com.pos.server.model.StockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Stock Log management
 */
@Repository
public interface StockLogRepository extends JpaRepository<StockLog, String> {
    List<StockLog> findByProductId(String productId);

    List<StockLog> findByType(String type);

    List<StockLog> findByUser(String user);

    List<StockLog> findAllByRestaurantId(String restaurantId);
}
