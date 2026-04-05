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
    @com.fasterxml.jackson.annotation.JsonProperty("id")
    private String productId; // Matches frontend 'id' for product reference (Product ID)

    private String name;
    private double price;
    private int qty;

    @Column(name = "unit_label")
    private String unitLabel; // e.g. '250 G', 'Half Plate'

    private Double multiplier; // e.g. 0.25 (optional, 1.0 if null)

    @Column(name = "cart_id")
    private String cartId; // Unique ID for variant/modifier combo

    // Tax Details
    @Column(name = "tax_rate")
    private Double taxRate;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "cgst_amount")
    private Double cgstAmount = 0.0;

    @Column(name = "sgst_amount")
    private Double sgstAmount = 0.0;

    @Column(name = "taxable_value")
    private Double taxableValue;

    @Column(name = "hsn_code")
    private String hsnCode = "9963";

    @Column(name = "tax_category")
    private String taxCategory;

    // Simplified modifiers storage to avoid join complexity and LOB issues
    @Column(name = "modifiers_json", columnDefinition = "TEXT")
    private String modifiersJson;

    @Transient
    private List<String> modifiers;

    @PostLoad
    private void loadModifiers() {
        if (modifiersJson != null && !modifiersJson.isEmpty()) {
            try {
                this.modifiers = new com.fasterxml.jackson.databind.ObjectMapper().readValue(modifiersJson,
                        java.util.List.class);
            } catch (Exception e) {
                this.modifiers = new java.util.ArrayList<>();
            }
        } else {
            this.modifiers = new java.util.ArrayList<>();
        }
    }

    @PrePersist
    @PreUpdate
    private void saveModifiers() {
        try {
            this.modifiersJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(modifiers);
        } catch (Exception e) {
            this.modifiersJson = "[]";
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;
}
