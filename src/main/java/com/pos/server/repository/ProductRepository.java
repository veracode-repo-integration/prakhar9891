package com.pos.server.repository;

import com.pos.server.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findAllByRestaurantId(String restaurantId);
    List<Product> findAllByRestaurantIdIgnoreCase(String restaurantId);
    List<Product> findAllByRestaurantIdIgnoreCaseOrderByCategoryAscNameAsc(String restaurantId);

    Optional<Product> findByIdAndRestaurantId(String id, String restaurantId);
    Optional<Product> findByIdAndRestaurantIdIgnoreCase(String id, String restaurantId);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(p.updatedAt) FROM Product p WHERE p.restaurantId = :restaurantId")
    java.time.LocalDateTime findLatestUpdateByRestaurantId(String restaurantId);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.image = NULL WHERE p.image IS NOT NULL")
    int clearAllProductImagesInDb();
}
