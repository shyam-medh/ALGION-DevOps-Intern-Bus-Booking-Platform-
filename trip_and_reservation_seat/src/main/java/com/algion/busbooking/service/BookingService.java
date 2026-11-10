package com.algion.busbooking.service;

import com.algion.busbooking.dto.BookingResponse;
import com.algion.busbooking.dto.CreateBookingRequest;
import com.algion.busbooking.dto.PassengerRequest;
import com.algion.busbooking.entity.Booking;
import com.algion.busbooking.entity.Passenger;
import com.algion.busbooking.entity.Trip;
import com.algion.busbooking.entity.TripSeat;
import com.algion.busbooking.exception.InvalidBookingException;
import com.algion.busbooking.exception.ResourceNotFoundException;
import com.algion.busbooking.exception.SeatAlreadyBookedException;
import com.algion.busbooking.repository.BookingRepository;
import com.algion.busbooking.repository.PassengerRepository;
import com.algion.busbooking.repository.TripRepository;
import com.algion.busbooking.repository.TripSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final TripSeatRepository tripSeatRepository;

    private static final String PNR_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public BookingService(BookingRepository bookingRepository,
                          PassengerRepository passengerRepository,
                          TripRepository tripRepository,
                          TripSeatRepository tripSeatRepository) {
        this.bookingRepository = bookingRepository;
        this.passengerRepository = passengerRepository;
        this.tripRepository = tripRepository;
        this.tripSeatRepository = tripSeatRepository;
    }

    /**
     * ATOMIC BOOKING CREATION WITH PESSIMISTIC LOCKING:
     * Prevents race conditions during concurrent seat reservation attempts.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(CreateBookingRequest request) {
        // 1. Validate Business Constraints
        validateBookingRequest(request);

        // 2. Fetch Trip
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with ID: " + request.getTripId()));

        if (!"SCHEDULED".equalsIgnoreCase(trip.getStatus())) {
            throw new InvalidBookingException("Trip is not available for booking. Status: " + trip.getStatus());
        }

        // 3. Atomically acquire lock and reserve each seat
        List<TripSeat> reservedSeats = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PassengerRequest passenger : request.getPassengers()) {
            String seatNum = passenger.getSeatNumber().trim().toUpperCase();

            // Row-level lock acquisition (SELECT ... FOR UPDATE)
            TripSeat tripSeat = tripSeatRepository.findByTripIdAndSeatNumberWithLock(trip.getId(), seatNum)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat " + seatNum + " does not exist on trip " + trip.getId()));

            if (!"AVAILABLE".equalsIgnoreCase(tripSeat.getStatus())) {
                throw new SeatAlreadyBookedException(seatNum);
            }

            // Mark seat booked
            tripSeat.setStatus("BOOKED");
            reservedSeats.add(tripSeat);
            totalAmount = totalAmount.add(tripSeat.getFare());
        }

        // Persist seat state changes
        tripSeatRepository.saveAll(reservedSeats);

        // 4. Generate Unique Secure PNR (e.g. ALG-2026-X8K2)
        String pnr = generateUniquePnr();

        // 5. Persist Booking
        Booking booking = new Booking(
                pnr,
                trip,
                request.getCustomerName().trim(),
                request.getCustomerEmail().trim().toLowerCase(),
                request.getCustomerPhone().trim(),
                totalAmount
        );
        booking = bookingRepository.save(booking);

        // 6. Persist Passengers
        List<Passenger> passengers = new ArrayList<>();
        for (PassengerRequest pReq : request.getPassengers()) {
            Passenger passenger = new Passenger(
                    booking,
                    pReq.getFullName().trim(),
                    pReq.getAge(),
                    pReq.getGender().trim().toUpperCase(),
                    pReq.getSeatNumber().trim().toUpperCase()
            );
            passengers.add(passenger);
        }
        passengerRepository.saveAll(passengers);

        return mapToBookingResponse(booking, passengers);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        List<Passenger> passengers = passengerRepository.findByBookingId(booking.getId());
        return mapToBookingResponse(booking, passengers);
    }

    @Transactional
    public BookingResponse cancelBooking(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with PNR: " + pnr));

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new InvalidBookingException("Booking " + pnr + " is already cancelled");
        }

        List<Passenger> passengers = passengerRepository.findByBookingId(booking.getId());

        // Release seats back to AVAILABLE
        for (Passenger passenger : passengers) {
            tripSeatRepository.findByTripIdAndSeatSeatNumber(booking.getTrip().getId(), passenger.getSeatNumber())
                    .ifPresent(tripSeat -> {
                        tripSeat.setStatus("AVAILABLE");
                        tripSeatRepository.save(tripSeat);
                    });
        }

        booking.setStatus("CANCELLED");
        booking.setUpdatedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        return mapToBookingResponse(booking, passengers);
    }

    private void validateBookingRequest(CreateBookingRequest request) {
        if (request.getTravelDate().isBefore(LocalDate.now())) {
            throw new InvalidBookingException("Travel date cannot be in the past: " + request.getTravelDate());
        }

        if (request.getBoardingStop().trim().equalsIgnoreCase(request.getDropStop().trim())) {
            throw new InvalidBookingException("Boarding stop and dropping stop cannot be identical");
        }

        // Check for duplicate seat selections within the same request
        Set<String> requestedSeats = new HashSet<>();
        for (PassengerRequest p : request.getPassengers()) {
            String seat = p.getSeatNumber().trim().toUpperCase();
            if (!requestedSeats.add(seat)) {
                throw new InvalidBookingException("Duplicate seat number requested in booking: " + seat);
            }
        }
    }

    private String generateUniquePnr() {
        String pnr;
        int attempts = 0;
        do {
            if (++attempts > 10) {
                pnr = "ALG-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                break;
            }
            StringBuilder sb = new StringBuilder("ALG-").append(Year.now().getValue()).append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(PNR_CHARS.charAt(secureRandom.nextInt(PNR_CHARS.length())));
            }
            pnr = sb.toString();
        } while (bookingRepository.existsByPnr(pnr));

        return pnr;
    }

    private BookingResponse mapToBookingResponse(Booking booking, List<Passenger> passengers) {
        BookingResponse response = new BookingResponse();
        response.setPnr(booking.getPnr());
        response.setStatus(booking.getStatus());
        response.setTripId(booking.getTrip().getId());
        response.setOrigin(booking.getTrip().getRoute().getOrigin());
        response.setDestination(booking.getTrip().getRoute().getDestination());
        response.setDepartureTime(booking.getTrip().getDepartureTime());
        response.setArrivalTime(booking.getTrip().getArrivalTime());
        response.setCustomerName(booking.getCustomerName());
        response.setCustomerEmail(booking.getCustomerEmail());
        response.setCustomerPhone(booking.getCustomerPhone());
        response.setTotalAmount(booking.getTotalAmount());
        response.setCreatedAt(booking.getCreatedAt());

        List<BookingResponse.PassengerTicketDto> ticketDtos = passengers.stream()
                .map(p -> new BookingResponse.PassengerTicketDto(p.getFullName(), p.getAge(), p.getGender(), p.getSeatNumber()))
                .collect(Collectors.toList());
        response.setPassengers(ticketDtos);

        return response;
    }
}

