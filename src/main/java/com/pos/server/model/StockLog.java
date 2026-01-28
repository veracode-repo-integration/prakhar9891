package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

/**
 * Stock Log entity for tracking inventory movements
 * Matches frontend StockLog structure from store.js
 */
@Entity
@Data
@Table(name = "stock_logs")
public class StockLog {
    @Id
    private String id;

    @Column(nullable = false)
    private String productId;

    private int qtyChange; // Positive or negative

    @Column(nullable = false)
    private String type; // 'sale', 'restock', 'correction', 'return'

    private String reason;
    private String user;

    @Column(nullable = false)
    private String createdAt; // ISO string
}
