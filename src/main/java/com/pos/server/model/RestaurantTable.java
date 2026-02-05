package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

/**
 * Table entity for restaurant table management
 * Matches frontend Table structure from store.js
 */
@Entity
@Data
@Table(name = "pos_tables")
public class RestaurantTable {
    @Id
    private String id;

    private String name;
    private String area;

    @Column(nullable = false)
    private String status = "available"; // 'available' or 'occupied'

    private int capacity = 4;

    @Column(columnDefinition = "TEXT")
    private String currentOrder; // JSON array of cart items

    private int guestCount = 0;
    private Long sessionStart;
    private String kitchenStatus;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_mobile")
    private String customerMobile;

    @Column(name = "restaurant_id")
    private String restaurantId;
}
