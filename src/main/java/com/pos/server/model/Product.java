package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private double price;
    private String category;
    private int stock;
    private String image;
    private String barcode;

    // For composite products logic if needed later
    @Column(columnDefinition = "TEXT")
    private String composition; // JSON string or logic to be handled

    private String restaurantId;
}
