package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * Order entity matching frontend Order structure.
 * Normalized for production use with proper relational mapping.
 */
@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    private String id; // UUID or unique readable ID

    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // Pricing
    private double subtotal;
    
    // Normalized Discount
    private String discountType; // 'amount', 'percent'
    private double discountValue;

    private double taxRate;
    private double taxAmount;
    private double total;

    // Timestamps
    private String date; // ISO string
    private long createdAt = System.currentTimeMillis();

    // Payment Relationship
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentDetail> payments = new ArrayList<>();

    private String method; // Primary method for quick reporting
    private String paymentId;

    // Customer Relationship
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // User Relationship
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User creator;
    
    private String user; // Username snapshot for display

    // Status
    @Column(nullable = false)
    private String status = "completed"; 

    private boolean isRefunded = false;

    // Pay Later
    private boolean payLater = false;
    private double payLaterAmount = 0.0;

    // Table & Order Type
    private String tableId;

    @Column(nullable = false)
    private String orderType = "dine-in";

    // Kitchen Status
    private String kitchenStatus = "pending";

    // Loyalty
    private int pointsEarned = 0;

    // Refund Tracking
    private double partialRefundAmount = 0.0;
    private double refundedTaxAmount = 0.0;
    private String refundedAt;
    private String refundMethod;
    
    @Column(columnDefinition = "TEXT")
    private String refundedItemsJson; // Keep as JSON for audit or normalize to Refund entity if needed
}
