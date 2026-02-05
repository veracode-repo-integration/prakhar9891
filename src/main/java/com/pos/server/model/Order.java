package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Order entity matching frontend Order structure.
 * Normalized for production use with proper relational mapping.
 */
@Entity
@Data
@Table(name = "pos_orders", indexes = {
        @Index(name = "idx_order_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_order_table", columnList = "table_id"),
        @Index(name = "idx_order_date", columnList = "created_at"),
        @Index(name = "idx_order_status", columnList = "order_status")
})
public class Order {
    @Id
    private String id;

    @Column(name = "invoice_number")
    private String invoiceNumber; // Legal sequence like INV-2025-001

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<OrderItem> items = new ArrayList<>();

    // Pricing
    private double subtotal;

    // Normalized Discount
    @Column(name = "discount_type")
    private String discountType; // 'amount', 'percent'

    @Column(name = "discount_value")
    private double discountValue;

    @Column(name = "tax_rate")
    private double taxRate;

    @Column(name = "tax_amount")
    private double taxAmount;

    private double total;

    // Timestamps
    @Column(name = "order_date")
    private String date; // ISO string

    @Column(name = "created_at")
    private long createdAt = System.currentTimeMillis();

    // Payment Relationship
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<PaymentDetail> payments = new ArrayList<>();

    @Column(name = "payment_method")
    private String method; // Primary method for quick reporting

    @Column(name = "payment_id")
    private String paymentId;

    // Customer Relationship
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private Customer customer;

    // User Relationship
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private User creator;

    @Column(name = "terminal_user")
    private String user; // Username snapshot for display

    // Status
    @Column(name = "order_status", nullable = false)
    private String status = "completed";

    @Column(name = "is_refunded")
    private boolean isRefunded = false;

    // Pay Later
    @Column(name = "pay_later")
    private boolean payLater = false;

    @Column(name = "pay_later_amount")
    private double payLaterAmount = 0.0;

    // Table & Order Type
    @Column(name = "table_id")
    private String tableId;

    @Column(name = "order_type", nullable = false)
    private String orderType = "dine-in";

    // Kitchen Status
    @Column(name = "kitchen_status")
    private String kitchenStatus = "pending";

    // Loyalty
    @Column(name = "points_earned")
    private int pointsEarned = 0;

    // Refund Tracking
    @Column(name = "partial_refund_amount")
    private double partialRefundAmount = 0.0;

    @Column(name = "refunded_tax_amount")
    private double refundedTaxAmount = 0.0;

    @Column(name = "refunded_at")
    private String refundedAt;

    @Column(name = "refund_method")
    private String refundMethod;

    @Column(name = "refunded_items_json", columnDefinition = "TEXT")
    private String refundedItemsJson; // Keep as JSON for audit or normalize to Refund entity if needed

    @Column(name = "is_reusable")
    private Boolean reusable = false; // Used for refund logic to restock items

    @Column(name = "restaurant_id")
    private String restaurantId;
}
