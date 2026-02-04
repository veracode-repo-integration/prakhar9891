package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "pos_products")
public class Product {
    @Id
    private String id;
    private String name;
    private double price;
    private String category;
    private int stock;
    private String image;
    private String barcode;
    private double costPrice;
    private double tax;
    @Column(name = "tax_category")
    private String taxCategory;

    @Column(name = "hsn_code")
    private String hsnCode = "9963"; // Default for restaurant services

    @Column(columnDefinition = "TEXT")
    private String composition;

    @Column(name = "restaurant_id")
    private String restaurantId;
}
