package com.algion.busbooking.controller;

import com.algion.busbooking.dto.BookingResponse;
import com.algion.busbooking.dto.CreateBookingRequest;
import com.algion.busbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<BookingResponse> getBookingByPnr(@PathVariable String pnr) {
        return ResponseEntity.ok(bookingService.getBookingByPnr(pnr));
    }

    @PostMapping("/{pnr}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable String pnr) {
        return ResponseEntity.ok(bookingService.cancelBooking(pnr));
    }
}

