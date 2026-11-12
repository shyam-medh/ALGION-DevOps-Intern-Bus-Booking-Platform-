package com.algion.busbooking.controller;

import com.algion.busbooking.dto.SeatLayoutResponse;
import com.algion.busbooking.dto.TripResponse;
import com.algion.busbooking.service.TripService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> getTrips(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (origin != null && !origin.isBlank() && destination != null && !destination.isBlank()) {
            LocalDate travelDate = (date != null) ? date : LocalDate.now();
            return ResponseEntity.ok(tripService.searchTrips(origin.trim(), destination.trim(), travelDate));
        }
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatLayoutResponse>> getSeatLayout(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getSeatLayout(id));
    }
}

