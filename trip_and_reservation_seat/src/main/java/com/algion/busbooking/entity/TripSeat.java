package com.algion.busbooking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "trip_seats", uniqueConstraints = {
    @UniqueConstraint(name = "uk_trip_seat", columnNames = {"trip_id", "seat_id"})
}, indexes = {
    @Index(name = "idx_trip_seat_status", columnList = "trip_id, status")
})
public class TripSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE"; // AVAILABLE, BOOKED

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    public TripSeat() {
    }

    public TripSeat(Trip trip, Seat seat, BigDecimal fare) {
        this.trip = trip;
        this.seat = seat;
        this.fare = fare;
        this.status = "AVAILABLE";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }
}

