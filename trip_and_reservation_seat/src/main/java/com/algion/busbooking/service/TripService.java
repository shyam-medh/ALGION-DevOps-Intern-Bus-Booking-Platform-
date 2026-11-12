package com.algion.busbooking.service;

import com.algion.busbooking.dto.SeatLayoutResponse;
import com.algion.busbooking.dto.TripResponse;
import com.algion.busbooking.entity.Trip;
import com.algion.busbooking.entity.TripSeat;
import com.algion.busbooking.exception.ResourceNotFoundException;
import com.algion.busbooking.repository.TripRepository;
import com.algion.busbooking.repository.TripSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final TripSeatRepository tripSeatRepository;

    public TripService(TripRepository tripRepository, TripSeatRepository tripSeatRepository) {
        this.tripRepository = tripRepository;
        this.tripSeatRepository = tripSeatRepository;
    }

    public List<TripResponse> getAllTrips() {
        return tripRepository.findAll().stream().map(this::mapToTripResponse).collect(Collectors.toList());
    }

    public List<TripResponse> searchTrips(String origin, String destination, LocalDate travelDate) {
        LocalDateTime startOfDay = travelDate.atStartOfDay();
        LocalDateTime endOfDay = travelDate.atTime(LocalTime.MAX);

        List<Trip> trips = tripRepository.searchTrips(origin, destination, startOfDay, endOfDay);
        return trips.stream().map(this::mapToTripResponse).collect(Collectors.toList());
    }

    public TripResponse getTripById(UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with ID: " + tripId));
        return mapToTripResponse(trip);
    }

    public List<SeatLayoutResponse> getSeatLayout(UUID tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip not found with ID: " + tripId);
        }

        List<TripSeat> seats = tripSeatRepository.findByTripIdOrderBySeatSeatNumberAsc(tripId);
        return seats.stream().map(ts -> new SeatLayoutResponse(
                ts.getTrip().getId(),
                ts.getSeat().getSeatNumber(),
                ts.getSeat().getSeatType(),
                ts.getSeat().getDeck(),
                ts.getFare(),
                ts.getStatus()
        )).collect(Collectors.toList());
    }

    private TripResponse mapToTripResponse(Trip trip) {
        TripResponse response = new TripResponse();
        response.setId(trip.getId());
        response.setOrigin(trip.getRoute().getOrigin());
        response.setDestination(trip.getRoute().getDestination());
        response.setDepartureTime(trip.getDepartureTime());
        response.setArrivalTime(trip.getArrivalTime());
        response.setBaseFare(trip.getBaseFare());
        response.setBusType(trip.getBus().getBusType());
        response.setTotalSeats(trip.getBus().getTotalSeats());
        response.setStatus(trip.getStatus());

        long availableSeats = tripSeatRepository.findByTripIdAndStatus(trip.getId(), "AVAILABLE").size();
        response.setAvailableSeats(availableSeats);

        return response;
    }
}

