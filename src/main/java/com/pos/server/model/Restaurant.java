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

    private Long createdAt;
    private boolean active = true;
}
