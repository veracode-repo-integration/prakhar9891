package com.pos.server.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "pos_terminal_settings")
public class Setting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String settingsJson;

    @Column(name = "restaurant_id")
    private String restaurantId;
}
