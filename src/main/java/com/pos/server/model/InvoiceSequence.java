package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "pos_invoice_sequences")
public class InvoiceSequence {
    @Id
    @Column(name = "restaurant_id")
    private String restaurantId;

    @Column(name = "last_value")
    private long lastValue = 0;
}
