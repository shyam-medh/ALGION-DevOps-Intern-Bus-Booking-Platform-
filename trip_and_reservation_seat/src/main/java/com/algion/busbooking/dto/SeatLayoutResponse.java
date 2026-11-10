package com.algion.busbooking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class SeatLayoutResponse {

    private UUID tripId;
    private String seatNumber;
    private String seatType; // WINDOW, AISLE
    private String deck;     // LOWER, UPPER
    private BigDecimal fare;
    private String status;   // AVAILABLE, BOOKED

    public SeatLayoutResponse() {
    }

    public SeatLayoutResponse(UUID tripId, String seatNumber, String seatType, String deck, BigDecimal fare, String status) {
        this.tripId = tripId;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.deck = deck;
        this.fare = fare;
        this.status = status;
    }

    public UUID getTripId() {
        return tripId;
    }

    public void setTripId(UUID tripId) {
        this.tripId = tripId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getDeck() {
        return deck;
    }

    public void setDeck(String deck) {
        this.deck = deck;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

