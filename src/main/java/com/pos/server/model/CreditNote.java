package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

/**
 * Credit Note entity matching frontend CreditNote structure from store.js
 * Tracks customer credits and their usage status
 */
@Entity
@Data
@Table(name = "pos_credit_notes")
public class CreditNote {
    @Id
    private String id;

    private String customerId;

    @Column(nullable = false)
    private String customerName;

    private String mobile;

    private double amount;
    private String reason;

    // Timestamps
    private String createdAt; // ISO string
    private String usedAt; // ISO string
    private long date; // Timestamp (for backward compatibility)

    // Status
    private boolean isUsed = false;

    @Column(name = "restaurant_id")
    private String restaurantId;
}
