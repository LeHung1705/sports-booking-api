package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.SportType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "courts")
@Data
public class Court {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Venue venue;

    private String name;

    @Enumerated(EnumType.STRING)
    private SportType sport;
    @Column(precision = 15, scale = 2)
    private BigDecimal pricePerHour;

    private Boolean isActive = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters & Setters
}
