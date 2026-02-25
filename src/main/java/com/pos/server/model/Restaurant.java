package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "restaurants")
public class Restaurant {
    @Id
    private String id; // This will be the "slug" like 'burgers-junction'

    private String name;
    private String address;
    private String phone;
    private String email;
    private String logoUrl;

    // Subscription & Access Control
    private String status; // ACTIVE, SUSPENDED, TRIAL, INACTIVE
    @Column(name = "plan_name")
    private String planName; // FREE, BASIC, PREMIUM, ENTERPRISE

    @Column(name = "subscription_end_date")
    private Long subscriptionEndDate; // Unix timestamp

    @Column(name = "allowed_features", columnDefinition = "TEXT")
    private String allowedFeaturesJson;

    @Transient
    private java.util.List<String> allowedFeatures;

    @PostLoad
    private void loadFeatures() {
        if (allowedFeaturesJson != null && !allowedFeaturesJson.isEmpty()) {
            try {
                this.allowedFeatures = new com.fasterxml.jackson.databind.ObjectMapper().readValue(allowedFeaturesJson,
                        java.util.List.class);
            } catch (Exception e) {
                this.allowedFeatures = new java.util.ArrayList<>();
            }
        } else {
            this.allowedFeatures = new java.util.ArrayList<>();
        }
    }

    private void saveFeaturesInternal() {
        try {
            if (this.allowedFeatures == null || this.allowedFeatures.isEmpty()) {
                this.allowedFeatures = new java.util.ArrayList<>(java.util.Arrays.asList(
                        "POS", "ORDERS", "KITCHEN", "EXPENSES", "CUSTOMERS", "REPORTS", "TABLES"));
            }
            this.allowedFeaturesJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(allowedFeatures);
        } catch (Exception e) {
            this.allowedFeaturesJson = "[]";
        }
    }

    private Long createdAt;
    private Long updatedAt;
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = System.currentTimeMillis();
        updatedAt = System.currentTimeMillis();
        if (status == null)
            status = "ACTIVE";
        if (planName == null)
            planName = "FREE";
        saveFeaturesInternal();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
        saveFeaturesInternal();
    }
}
