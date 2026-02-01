package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "db_id")
    private Long dbId;

    @Column(name = "product_id")
    private String id; // Product ID reference

    private String name;
    private double price;
    private int qty;

    @Column(name = "cart_id")
    private String cartId; // Unique ID for variant/modifier combo

    @ElementCollection
    @CollectionTable(name = "order_item_modifiers", joinColumns = @JoinColumn(name = "order_item_id"))
    @Column(name = "modifier_name")
    private List<String> modifiers;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
