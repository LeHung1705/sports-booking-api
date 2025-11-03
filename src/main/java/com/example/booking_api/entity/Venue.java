package com.example.booking_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import lombok.*;

@Entity
@Table(name = "venues", indexes = @Index(columnList = "city, district"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 80)
    private String district;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    private Boolean isActive = true;
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "venue", fetch = FetchType.LAZY)
    private List<Court> courts;

    @OneToMany(mappedBy = "venue", fetch = FetchType.LAZY)
    private List<Review> reviews;
}
