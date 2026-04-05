package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pos_products", indexes = {
        @Index(name = "idx_prod_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_prod_category", columnList = "category"),
        @Index(name = "idx_prod_barcode", columnList = "barcode")
})
public class Product {
    @Id
    private String id;
    private String name;
    private double price;
    private String category;
    private double stock;
    private String image;
    private String barcode;
    private double costPrice;
    private double tax;
    @Column(name = "tax_category")
    private String taxCategory;

    @Column(name = "base_unit")
    private String baseUnit = "Pc"; // Smallest unit, e.g. Gram, Pc

    @Column(name = "display_unit")
    private String displayUnit = "Pcs"; // User-facing unit, e.g. KG, Pcs

    @Column(name = "hsn_code")
    private String hsnCode = "9963"; // Default for restaurant services

    @Column(columnDefinition = "TEXT")
    private String composition;

    @Column(name = "selling_units", columnDefinition = "TEXT")
    private String sellingUnits;

    @Column(name = "restaurant_id")
    private String restaurantId;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "external_mappings", columnDefinition = "TEXT")
    private String externalMappings; // JSON for aggregator IDs (Zomato, Swiggy)

    @Column(name = "online_availability")
    private String onlineAvailability = "available"; // 'available' or 'out_of_stock'
}
