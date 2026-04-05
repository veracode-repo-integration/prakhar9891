package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "support_tickets")
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id")
    private String restaurantId;

    @Column(name = "restaurant_name")
    private String restaurantName;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "user_name")
    private String userName;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priority; // LOW, MEDIUM, HIGH
    private String status; // OPEN, RESOLVED

    private Long createdAt;
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
        if (status == null) status = "OPEN";
        if (priority == null) priority = "MEDIUM";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }
}
