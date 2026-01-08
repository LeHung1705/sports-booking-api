package com.example.booking_api.event;

import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.enums.NotificationType; // Nhớ import Enum
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookingEvent extends ApplicationEvent {
    private final Booking booking;
    private final NotificationType type;

    public BookingEvent(Object source, Booking booking, NotificationType type) {
        super(source);
        this.booking = booking;
        this.type = type;
    }
}