package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "customers")
public class Customer {
    @Id
    private String id;
    private String name;
    private String mobile;
    private String email;
    private String address;
    private String city;
    private String type; // VIP, Regular
}
