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
    private String businessType; // NEW: To store business nature (Cafe, Hotel, etc.)

    // Subscription & Access Control
    private String status; // ACTIVE, SUSPENDED, TRIAL, INACTIVE
    @Column(name = "plan_name")
    private String planName; // FREE, BASIC, PREMIUM, ENTERPRISE

    @Column(name = "subscription_end_date")
    private Long subscriptionEndDate; // Unix timestamp

    @Column(name = "last_paid_date")
    private Long lastPaidDate; // Unix timestamp of last manual payment

    @Column(name = "next_due_date")
    private Long nextDueDate; // Unix timestamp of next renewal due

    @Column(name = "trial_end_date")
    private Long trialEndDate; // Unix timestamp when trial expires

    @Column(name = "subscription_note", columnDefinition = "TEXT")
    private String subscriptionNote; // Admin notes

    @Column(name = "subscription_months")
    private Integer subscriptionMonths; // Duration of last paid subscription (1,2,3,6,12)

    @Column(name = "amount_paid")
    private Double amountPaid; // Actual amount received (base excl. GST)

    @Column(name = "amount_paid_with_gst")
    private Double amountPaidWithGst; // Actual amount received (incl. GST)

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

    @Column(name = "online_status")
    private String onlineStatus = "OPEN"; // OPEN, CLOSED
}
