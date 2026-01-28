package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

/**
 * Customer entity matching frontend Customer structure from store.js
 * Includes loyalty points and wallet balance for customer rewards
 */
@Entity
@Data
@Table(name = "customers")
public class Customer {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mobile;

    private String email;
    private String address;
    private String city;
    private String type; // 'VIP', 'Regular', 'New'

    // Loyalty & Rewards
    private int loyaltyPoints = 0;
    private double walletBalance = 0.0;

    // Timestamp
    private String joinedAt; // ISO string
}
