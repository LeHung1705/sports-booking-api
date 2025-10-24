package com.example.booking_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "venues", indexes = @Index(columnList = "city, district"))
@Data
public class Venue {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String name;
    private String address;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String description;
    private Boolean isActive = true;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
