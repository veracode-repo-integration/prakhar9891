package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

/**
 * Held Order entity for parked/held orders
 * Matches frontend HeldOrder structure from store.js
 */
@Entity
@Data
@Table(name = "held_orders")
public class HeldOrder {
    @Id
    private String id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String items; // JSON array of cart items

    private String note;

    @Column(nullable = false)
    private String createdAt; // ISO string

    private String restaurantId;
}
