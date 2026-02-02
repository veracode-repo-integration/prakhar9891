package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "pos_order_items")
public class OrderItem {
    @Id
    @Column(name = "item_id")
    private String itemId; // Order Line Unique ID

    @Column(name = "product_id")
    private String id; // Matches frontend 'id' for product reference (Product ID)

    private String name;
    private double price;
    private int qty;

    @Column(name = "cart_id")
    private String cartId; // Unique ID for variant/modifier combo

    @ElementCollection
    @CollectionTable(name = "pos_order_item_modifiers", joinColumns = @JoinColumn(name = "order_item_id", referencedColumnName = "item_id"))
    @Column(name = "modifier_name")
    private List<String> modifiers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;
}
