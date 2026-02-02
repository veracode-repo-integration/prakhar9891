package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "pos_credit_transactions")
public class CreditTransaction {
    @Id
    private String id;

    private String mobile;
    private String name;

    private String type; // 'order' or 'payment' or 'refund'
    private double amount;
    private long date;

    private String orderId; // Linked order ID if type is 'order' or 'refund'
    private String note;

    @Column(name = "restaurant_id")
    private String restaurantId;
}
