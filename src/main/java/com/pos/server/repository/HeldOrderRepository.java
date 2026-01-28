package com.pos.server.repository;

import com.pos.server.model.HeldOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Held Order (Parked Order) management
 */
@Repository
public interface HeldOrderRepository extends JpaRepository<HeldOrder, String> {
    // All basic CRUD operations inherited from JpaRepository
}
