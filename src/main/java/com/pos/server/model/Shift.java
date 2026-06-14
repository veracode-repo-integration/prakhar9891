package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "pos_shifts", indexes = {
        @Index(name = "idx_shift_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_shift_user", columnList = "user_id"),
        @Index(name = "idx_shift_status", columnList = "status")
})
public class Shift {
    @Id
    private String id;

    @Column(name = "restaurant_id", nullable = false)
    private String restaurantId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "open_time", nullable = false)
    private long openTime;

    @Column(name = "close_time")
    private Long closeTime;

    @Column(name = "opening_balance")
    private double openingBalance = 0.0;

    @Column(name = "cash_sales")
    private double cashSales = 0.0;

    @Column(name = "upi_sales")
    private double upiSales = 0.0;

    @Column(name = "card_sales")
    private double cardSales = 0.0;

    @Column(name = "expected_cash")
    private double expectedCash = 0.0;

    @Column(name = "actual_cash")
    private double actualCash = 0.0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private String status = "OPEN"; // 'OPEN' or 'CLOSED'
}
