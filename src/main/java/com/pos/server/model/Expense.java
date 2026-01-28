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
    private String name; // Who spent it

    // Legacy support for DB constraint
    private String title;

    private double amount;
    private String category;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private String mobile;

    // Timestamp
    private String createdAt;

    @PrePersist
    public void prePersist() {
        if (this.title == null) {
            this.title = (this.name != null) ? this.name : "Expense";
        }
        if (this.createdAt == null) {
            this.createdAt = java.time.Instant.now().toString();
        }
    }
}
