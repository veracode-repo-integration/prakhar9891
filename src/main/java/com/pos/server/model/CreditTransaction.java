package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "credit_transactions")
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

    private String restaurantId;
}
