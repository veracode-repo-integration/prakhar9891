package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "credit_notes")
public class CreditNote {
    @Id
    private String id;
    private String customerId;
    private String customerName;
    private double amount;
    private String reason;
    private long date;
}
