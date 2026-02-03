package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

/**
 * Customer entity matching frontend Customer structure from store.js
 * Includes loyalty points and wallet balance for customer rewards
 */
@Entity
@Data
@Table(name = "pos_customers")
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
    @Column(name = "customer_type")
    private String type; // 'VIP', 'Regular', 'New'

    // Loyalty & Rewards
    private int loyaltyPoints = 0;
    private double walletBalance = 0.0;

    // Timestamp
    private String joinedAt; // ISO string

    @Column(name = "restaurant_id")
    private String restaurantId;
}
