package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "expenses")
public class Expense {
    @Id
    private String id;
    private String title;
    private double amount;
    private String category;
    private String date; // ISO Date or timestamp
    private String notes;
}
