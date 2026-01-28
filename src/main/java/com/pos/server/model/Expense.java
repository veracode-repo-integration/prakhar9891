package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

/**
 * Expense entity matching frontend Expense structure from store.js
 */
@Entity
@Data
@Table(name = "expenses")
public class Expense {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    private double amount;
    private String category;
    private String notes;

    // Timestamp
    private String createdAt; // ISO string
    private String date; // ISO Date (for backward compatibility)
}
