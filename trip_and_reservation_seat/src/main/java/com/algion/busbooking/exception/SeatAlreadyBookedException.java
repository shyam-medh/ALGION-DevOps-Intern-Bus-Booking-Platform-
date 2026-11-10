package com.algion.busbooking.exception;

public class SeatAlreadyBookedException extends RuntimeException {

    private final String seatNumber;

    public SeatAlreadyBookedException(String seatNumber) {
        super("Seat " + seatNumber + " is already booked or currently unavailable");
        this.seatNumber = seatNumber;
    }

    public SeatAlreadyBookedException(String seatNumber, String message) {
        super(message);
        this.seatNumber = seatNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}

