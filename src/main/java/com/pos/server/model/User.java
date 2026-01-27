package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dbId;

    private String username;
    private String passwordHash; // BCrypt
    private String firstName;
    private String lastName;
    private String email;
    private String contact;
    private String role; // admin, staff
}
