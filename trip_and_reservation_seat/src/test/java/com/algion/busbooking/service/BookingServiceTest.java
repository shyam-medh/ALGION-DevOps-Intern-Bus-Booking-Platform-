package com.algion.busbooking.service;

import com.algion.busbooking.dto.BookingResponse;
import com.algion.busbooking.dto.CreateBookingRequest;
import com.algion.busbooking.dto.PassengerRequest;
import com.algion.busbooking.entity.*;
import com.algion.busbooking.exception.InvalidBookingException;
import com.algion.busbooking.exception.SeatAlreadyBookedException;
import com.algion.busbooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

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

    private Trip testTrip;

    @BeforeEach
    void setUp() {
        passengerRepository.deleteAll();
        bookingRepository.deleteAll();
        tripSeatRepository.deleteAll();
        tripRepository.deleteAll();
        seatRepository.deleteAll();
        busRepository.deleteAll();
        routeRepository.deleteAll();

        Route route = routeRepository.save(new Route("Delhi", "Jaipur", 280.0, 300));
        Bus bus = busRepository.save(new Bus("DL-01-AB-9999", "AC_SLEEPER", 30));
        Seat seat1A = seatRepository.save(new Seat(bus, "1A", "WINDOW", "LOWER"));
        Seat seat1B = seatRepository.save(new Seat(bus, "1B", "AISLE", "LOWER"));

        LocalDateTime departure = LocalDateTime.now().plusDays(2);
        testTrip = tripRepository.save(new Trip(route, bus, departure, departure.plusHours(5), BigDecimal.valueOf(750.00)));

        tripSeatRepository.save(new TripSeat(testTrip, seat1A, BigDecimal.valueOf(750.00)));
        tripSeatRepository.save(new TripSeat(testTrip, seat1B, BigDecimal.valueOf(750.00)));
    }

    @Test
    @DisplayName("Should successfully create booking and generate valid PNR")
    void testSuccessfulBooking() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTripId(testTrip.getId());
        request.setTravelDate(LocalDate.now().plusDays(2));
        request.setBoardingStop("Delhi ISBT");
        request.setDropStop("Jaipur Sindhi Camp");
        request.setCustomerName("Pooja Verma");
        request.setCustomerEmail("pooja@example.com");
        request.setCustomerPhone("+919123456789");

        List<PassengerRequest> passengers = List.of(
                new PassengerRequest("Pooja Verma", 25, "FEMALE", "1A")
        );
        request.setPassengers(passengers);

        BookingResponse response = bookingService.createBooking(request);

        assertThat(response).isNotNull();
        assertThat(response.getPnr()).startsWith("ALG-");
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getPassengers()).hasSize(1);
        assertThat(response.getPassengers().get(0).getSeatNumber()).isEqualTo("1A");

        // Verify Seat is now BOOKED in DB
        TripSeat bookedSeat = tripSeatRepository.findByTripIdAndSeatSeatNumber(testTrip.getId(), "1A").orElseThrow();
        assertThat(bookedSeat.getStatus()).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("Should reject booking if travel date is in the past")
    void testPastTravelDate() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTripId(testTrip.getId());
        request.setTravelDate(LocalDate.now().minusDays(1)); // Past date
        request.setBoardingStop("Delhi");
        request.setDropStop("Jaipur");
        request.setCustomerName("Test User");
        request.setCustomerEmail("test@example.com");
        request.setCustomerPhone("+919000000000");
        request.setPassengers(List.of(new PassengerRequest("Test", 30, "MALE", "1A")));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("Travel date cannot be in the past");
    }

    @Test
    @DisplayName("Should reject booking if duplicate seat is requested in same request")
    void testDuplicateSeatInRequest() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTripId(testTrip.getId());
        request.setTravelDate(LocalDate.now().plusDays(2));
        request.setBoardingStop("Delhi");
        request.setDropStop("Jaipur");
        request.setCustomerName("Test User");
        request.setCustomerEmail("test@example.com");
        request.setCustomerPhone("+919000000000");
        request.setPassengers(List.of(
                new PassengerRequest("Passenger 1", 30, "MALE", "1A"),
                new PassengerRequest("Passenger 2", 28, "FEMALE", "1A") // Duplicate seat
        ));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("Duplicate seat number");
    }

    @Test
    @DisplayName("Should reject sequential double booking on already booked seat")
    void testSequentialDoubleBooking() {
        CreateBookingRequest request1 = new CreateBookingRequest();
        request1.setTripId(testTrip.getId());
        request1.setTravelDate(LocalDate.now().plusDays(2));
        request1.setBoardingStop("Delhi");
        request1.setDropStop("Jaipur");
        request1.setCustomerName("User One");
        request1.setCustomerEmail("user1@example.com");
        request1.setCustomerPhone("+919111111111");
        request1.setPassengers(List.of(new PassengerRequest("User One", 30, "MALE", "1A")));

        bookingService.createBooking(request1);

        // Second user attempts to book same seat 1A
        CreateBookingRequest request2 = new CreateBookingRequest();
        request2.setTripId(testTrip.getId());
        request2.setTravelDate(LocalDate.now().plusDays(2));
        request2.setBoardingStop("Delhi");
        request2.setDropStop("Jaipur");
        request2.setCustomerName("User Two");
        request2.setCustomerEmail("user2@example.com");
        request2.setCustomerPhone("+919222222222");
        request2.setPassengers(List.of(new PassengerRequest("User Two", 25, "FEMALE", "1A")));

        assertThatThrownBy(() -> bookingService.createBooking(request2))
                .isInstanceOf(SeatAlreadyBookedException.class)
                .hasMessageContaining("Seat 1A is already booked");
    }

    @Test
    @DisplayName("Should cancel booking and release seat back to AVAILABLE")
    void testCancelBooking() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTripId(testTrip.getId());
        request.setTravelDate(LocalDate.now().plusDays(2));
        request.setBoardingStop("Delhi");
        request.setDropStop("Jaipur");
        request.setCustomerName("User Cancel");
        request.setCustomerEmail("cancel@example.com");
        request.setCustomerPhone("+919333333333");
        request.setPassengers(List.of(new PassengerRequest("User Cancel", 30, "MALE", "1A")));

        BookingResponse response = bookingService.createBooking(request);
        String pnr = response.getPnr();

        // Cancel
        BookingResponse cancelResponse = bookingService.cancelBooking(pnr);
        assertThat(cancelResponse.getStatus()).isEqualTo("CANCELLED");

        // Verify Seat is released back to AVAILABLE
        TripSeat seat = tripSeatRepository.findByTripIdAndSeatSeatNumber(testTrip.getId(), "1A").orElseThrow();
        assertThat(seat.getStatus()).isEqualTo("AVAILABLE");
    }

    /**
     * CRITICAL ASSESSMENT ACCEPTANCE TEST:
     * "A conflicting seat booking must fail safely and must not create a duplicate PNR."
     * 10 concurrent threads try to book the exact same seat simultaneously.
     * Exactly 1 must succeed, 9 must fail with SeatAlreadyBookedException, and exactly 1 PNR must exist.
     */
    @Test
    @DisplayName("Acceptance Test: Concurrent conflicting seat booking must fail safely with zero duplicate PNRs")
    void testConcurrentConflictingSeatBooking() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<String> generatedPnrs = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int userIndex = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for simultaneous release

                    CreateBookingRequest request = new CreateBookingRequest();
                    request.setTripId(testTrip.getId());
                    request.setTravelDate(LocalDate.now().plusDays(2));
                    request.setBoardingStop("Delhi");
                    request.setDropStop("Jaipur");
                    request.setCustomerName("Concurrent User " + userIndex);
                    request.setCustomerEmail("user" + userIndex + "@test.com");
                    request.setCustomerPhone("+91900000000" + userIndex);
                    request.setPassengers(List.of(new PassengerRequest("Passenger " + userIndex, 25, "MALE", "1A")));

                    BookingResponse response = bookingService.createBooking(request);
                    successCount.incrementAndGet();
                    generatedPnrs.add(response.getPnr());
                } catch (SeatAlreadyBookedException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected exception
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 10 threads simultaneously

        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        // ASSERTIONS
        assertThat(successCount.get()).isEqualTo(1); // EXACTLY ONE succeeds
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1); // EXACTLY N-1 safely fail
        assertThat(generatedPnrs).hasSize(1); // EXACTLY ONE PNR created
        assertThat(bookingRepository.count()).isEqualTo(1); // Database contains exactly 1 booking record
    }
}

