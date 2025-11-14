package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.SportType;
import jakarta.persistence.*;
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
    private Venue venue;

    private String name;

    @Enumerated(EnumType.STRING)
    private SportType sport;

    private Double pricePerHour;

    private Boolean isActive = true;

    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters & Setters
}
