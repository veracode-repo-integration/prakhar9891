package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "payment_details")
public class PaymentDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_method")
    private String method; // 'Cash', 'UPI', 'Card', 'Pay Later'

    private double amount;
    private String reference; // Transaction ID or note

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
