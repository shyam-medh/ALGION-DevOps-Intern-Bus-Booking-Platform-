package com.algion.busbooking.db;

import com.algion.busbooking.entity.*;
import com.algion.busbooking.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;
    private final TripSeatRepository tripSeatRepository;

    public DataSeeder(RouteRepository routeRepository,
                      BusRepository busRepository,
                      SeatRepository seatRepository,
                      TripRepository tripRepository,
                      TripSeatRepository tripSeatRepository) {
        this.routeRepository = routeRepository;
        this.busRepository = busRepository;
        this.seatRepository = seatRepository;
        this.tripRepository = tripRepository;
        this.tripSeatRepository = tripSeatRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (routeRepository.count() > 0) {
            return;
        }

        // 1. Routes
        Route r1 = routeRepository.save(new Route("Mumbai", "Pune", 150.0, 180));
        Route r2 = routeRepository.save(new Route("Bangalore", "Chennai", 350.0, 360));
        Route r3 = routeRepository.save(new Route("Delhi", "Jaipur", 280.0, 300));

        // 2. Buses
        Bus bus1 = busRepository.save(new Bus("MH-12-AB-1234", "AC_SLEEPER", 20));
        Bus bus2 = busRepository.save(new Bus("KA-01-CD-5678", "AC_SEATER", 30));

        // 3. Seats for Bus 1 (20 Sleeper Seats: 1A-10A Window, 1B-10B Aisle)
        List<Seat> bus1Seats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            bus1Seats.add(new Seat(bus1, i + "A", "WINDOW", "LOWER"));
            bus1Seats.add(new Seat(bus1, i + "B", "AISLE", "LOWER"));
        }
        seatRepository.saveAll(bus1Seats);

        // Seats for Bus 2 (30 Seater Seats: 1A-10A Window, 1B-10B Middle, 1C-10C Aisle)
        List<Seat> bus2Seats = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            bus2Seats.add(new Seat(bus2, i + "A", "WINDOW", "LOWER"));
            bus2Seats.add(new Seat(bus2, i + "B", "MIDDLE", "LOWER"));
            bus2Seats.add(new Seat(bus2, i + "C", "AISLE", "LOWER"));
        }
        seatRepository.saveAll(bus2Seats);

        // 4. Scheduled Trips
        LocalDate today = LocalDate.now();
        Trip trip1 = tripRepository.save(new Trip(
                r1,
                bus1,
                LocalDateTime.of(today, LocalTime.of(18, 0)),
                LocalDateTime.of(today, LocalTime.of(21, 0)),
                new BigDecimal("450.00")
        ));

        Trip trip2 = tripRepository.save(new Trip(
                r1,
                bus1,
                LocalDateTime.of(today.plusDays(1), LocalTime.of(8, 0)),
                LocalDateTime.of(today.plusDays(1), LocalTime.of(11, 0)),
                new BigDecimal("450.00")
        ));

        Trip trip3 = tripRepository.save(new Trip(
                r2,
                bus2,
                LocalDateTime.of(today, LocalTime.of(22, 0)),
                LocalDateTime.of(today.plusDays(1), LocalTime.of(4, 0)),
                new BigDecimal("750.00")
        ));

        // 5. Trip Seats
        createTripSeats(trip1, bus1Seats, new BigDecimal("450.00"));
        createTripSeats(trip2, bus1Seats, new BigDecimal("450.00"));
        createTripSeats(trip3, bus2Seats, new BigDecimal("750.00"));
    }

    private void createTripSeats(Trip trip, List<Seat> seats, BigDecimal baseFare) {
        List<TripSeat> tripSeats = new ArrayList<>();
        for (Seat seat : seats) {
            BigDecimal fare = baseFare;
            if ("WINDOW".equalsIgnoreCase(seat.getSeatType())) {
                fare = fare.add(new BigDecimal("50.00"));
            }
            tripSeats.add(new TripSeat(trip, seat, fare));
        }
        tripSeatRepository.saveAll(tripSeats);
    }
}

