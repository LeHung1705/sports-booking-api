package com.example.booking_api.repository;

import com.example.booking_api.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByVenueId(UUID venueId);

    interface VenuePriceAgg {
        UUID getVenueId();
        BigDecimal getMinPrice();
        BigDecimal getMaxPrice();
        BigDecimal getAvgPrice();
    }

    @Query("""
            SELECT c.venue.id AS venueId,
                   MIN(c.pricePerHour) AS minPrice,
                   MAX(c.pricePerHour) AS maxPrice,
                   AVG(c.pricePerHour) AS avgPrice
            FROM Court c
            WHERE c.isActive = true
              AND c.venue.id IN :venueIds
            GROUP BY c.venue.id
            """)
    List<VenuePriceAgg> getPriceAggByVenueIds(@Param("venueIds") List<UUID> venueIds);

    long countByVenue_Owner_Id(UUID ownerId);
}
