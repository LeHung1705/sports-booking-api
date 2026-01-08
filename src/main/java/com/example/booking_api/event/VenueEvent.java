package com.example.booking_api.event;

import com.example.booking_api.entity.Venue;
import com.example.booking_api.entity.enums.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VenueEvent extends ApplicationEvent {
    private final Venue venue;
    private final NotificationType type;

    public VenueEvent(Object source, Venue venue, NotificationType type) {
        super(source);
        this.venue = venue;
        this.type = type;
    }
}
