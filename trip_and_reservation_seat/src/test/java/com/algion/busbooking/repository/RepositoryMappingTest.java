package com.algion.busbooking.repository;

import com.algion.busbooking.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RepositoryMappingTest {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private BusRepository busRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripSeatRepository tripSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Test
    @DisplayName("Should persist entities and successfully execute pessimistic row lock query")
    void testEntitiesAndLockingQuery() {
        // 1. Create Route
        Route route = routeRepository.save(new Route("Mumbai", "Pune", 150.0, 180));
        assertThat(route.getId()).isNotNull();

        // 2. Create Bus & Seat
        Bus bus = busRepository.save(new Bus("MH-12-AB-1234", "AC_SEATER", 40));
        Seat seat = seatRepository.save(new Seat(bus, "1A", "WINDOW", "LOWER"));
        assertThat(seat.getId()).isNotNull();

        // 3. Create Trip & TripSeat
        LocalDateTime departure = LocalDateTime.now().plusDays(1);
        LocalDateTime arrival = departure.plusHours(3);
        Trip trip = tripRepository.save(new Trip(route, bus, departure, arrival, BigDecimal.valueOf(500.00)));

        TripSeat tripSeat = tripSeatRepository.save(new TripSeat(trip, seat, BigDecimal.valueOf(500.00)));
        assertThat(tripSeat.getId()).isNotNull();
        assertThat(tripSeat.getStatus()).isEqualTo("AVAILABLE");

        // 4. Test Pessimistic Lock Query
        Optional<TripSeat> lockedSeatOpt = tripSeatRepository.findByTripIdAndSeatNumberWithLock(trip.getId(), "1A");
        assertThat(lockedSeatOpt).isPresent();
        assertThat(lockedSeatOpt.get().getStatus()).isEqualTo("AVAILABLE");

        // 5. Create Booking & Passenger
        Booking booking = bookingRepository.save(new Booking(
                "ALG-2026-TEST1",
                trip,
                "Rahul Sharma",
                "rahul@example.com",
                "+919876543210",
                BigDecimal.valueOf(500.00)
        ));
        assertThat(booking.getId()).isNotNull();

        Passenger passenger = passengerRepository.save(new Passenger(
                booking,
                "Rahul Sharma",
                28,
                "MALE",
                "1A"
        ));
        assertThat(passenger.getId()).isNotNull();

        // 6. Test Lookup by PNR
        Optional<Booking> retrievedBooking = bookingRepository.findByPnr("ALG-2026-TEST1");
        assertThat(retrievedBooking).isPresent();
        assertThat(retrievedBooking.get().getCustomerEmail()).isEqualTo("rahul@example.com");
    }
}
