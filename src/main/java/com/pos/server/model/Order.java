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
@Table(name = "pos_orders")
public class Order {
    @Id
    private String id; // UUID or unique readable ID

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<OrderItem> items = new ArrayList<>();

    // Pricing
    private double subtotal;

    // ... (skipping unchanged lines) ...

    @Column(name = "refund_method")
    private String refundMethod;

    @Column(name = "refunded_items_json", columnDefinition = "TEXT")
    private String refundedItemsJson; // Keep as JSON for audit or normalize to Refund entity if needed

    @Column(name = "restaurant_id")
    private String restaurantId;
}
