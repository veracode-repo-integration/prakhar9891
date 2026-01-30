package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import java.util.List;

/**
 * Order entity matching frontend Order structure from store.js
 * Supports all POS features: discounts, pay later, table orders, loyalty points
 */
@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    private String id; // Order ID (e.g., "123456")

    private String note;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;

    // Pricing
    private double subtotal;

    @Column(columnDefinition = "TEXT")
    private String discountJson; // JSON: { type: 'amount'|'percent', value: number }

    private double taxRate;
    private double taxAmount;
    private double total;

    // Timestamps
    private String date; // ISO string
    private long createdAt; // Timestamp for backward compatibility

    // Payment
    @Column(columnDefinition = "TEXT")
    private String paymentInfoJson; // JSON: { method, payments, customer, note }

    private String method; // Primary payment method (for backward compatibility)
    private String paymentId;

    // Customer
    @Column(columnDefinition = "TEXT")
    private String customerJson; // JSON: customer object with loyaltyPoints, walletBalance

    // User
    private String user; // Username who created the order

    @Column(columnDefinition = "TEXT")
    private String userJson; // Full user info snapshot (for backward compatibility)

    // Status
    @Column(nullable = false)
    private String status = "completed"; // 'pending' or 'completed'

    private boolean isRefunded = false;

    // Pay Later
    private boolean payLater = false;
    private double payLaterAmount = 0.0;

    // Table & Order Type
    private String tableId;

    @Column(nullable = false)
    private String orderType = "dine-in"; // 'dine-in', 'takeaway', 'delivery'

    // Kitchen Status
    private String kitchenStatus = "pending"; // 'pending', 'preparing', 'ready', 'served'

    // Loyalty
    private int pointsEarned = 0;

    // Refund Tracking (New fields for production sync)
    private double partialRefundAmount = 0.0;
    private double refundedTaxAmount = 0.0;
    private String refundedAt;
    private String refundMethod;

    @Column(columnDefinition = "TEXT")
    private String refundedItemsJson; // JSON list of items that were refunded
}
