package com.pos.server.repository;

import com.pos.server.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId);
    Page<AuditLog> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId, Pageable pageable);
}
