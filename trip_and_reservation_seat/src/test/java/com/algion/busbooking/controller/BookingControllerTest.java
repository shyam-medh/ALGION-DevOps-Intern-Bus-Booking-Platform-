package com.algion.busbooking.controller;

import com.algion.busbooking.dto.BookingResponse;
import com.algion.busbooking.dto.CreateBookingRequest;
import com.algion.busbooking.dto.PassengerRequest;
import com.algion.busbooking.exception.GlobalExceptionHandler;
import com.algion.busbooking.exception.ResourceNotFoundException;
import com.algion.busbooking.exception.SeatAlreadyBookedException;
import com.algion.busbooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("POST /api/v1/bookings returns 201 Created on valid reservation")
    void createBooking_Success_Returns201() throws Exception {
        UUID tripId = UUID.randomUUID();
        CreateBookingRequest request = createValidBookingRequest(tripId);

        BookingResponse mockResponse = new BookingResponse();
        mockResponse.setPnr("ALG-2026-X8K2");
        mockResponse.setStatus("CONFIRMED");
        mockResponse.setTripId(tripId);
        mockResponse.setOrigin("Mumbai");
        mockResponse.setDestination("Pune");
        mockResponse.setDepartureTime(LocalDateTime.now().plusHours(2));
        mockResponse.setArrivalTime(LocalDateTime.now().plusHours(5));
        mockResponse.setCustomerName("Rahul Sharma");
        mockResponse.setCustomerEmail("rahul@example.com");
        mockResponse.setCustomerPhone("+919876543210");
        mockResponse.setTotalAmount(new BigDecimal("500.00"));
        mockResponse.setCreatedAt(LocalDateTime.now());
        mockResponse.setPassengers(List.of(
                new BookingResponse.PassengerTicketDto("Rahul Sharma", 28, "MALE", "1A")
        ));

        when(bookingService.createBooking(any(CreateBookingRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pnr").value("ALG-2026-X8K2"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.customerName").value("Rahul Sharma"))
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.passengers[0].seatNumber").value("1A"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings returns 409 Conflict when seat is already booked")
    void createBooking_SeatAlreadyBooked_Returns409() throws Exception {
        UUID tripId = UUID.randomUUID();
        CreateBookingRequest request = createValidBookingRequest(tripId);

        when(bookingService.createBooking(any(CreateBookingRequest.class)))
                .thenThrow(new SeatAlreadyBookedException("1A"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Seat 1A is already booked or currently unavailable"))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings"));
    }

    @Test
    @DisplayName("POST /api/v1/bookings returns 400 Bad Request when customer email is invalid")
    void createBooking_InvalidEmail_Returns400() throws Exception {
        UUID tripId = UUID.randomUUID();
        CreateBookingRequest request = createValidBookingRequest(tripId);
        request.setCustomerEmail("invalid-email-format");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/bookings/{pnr} returns 404 Not Found for non-existent PNR")
    void getBookingByPnr_NotFound_Returns404() throws Exception {
        String nonExistentPnr = "ALG-0000-NONE";
        when(bookingService.getBookingByPnr(nonExistentPnr))
                .thenThrow(new ResourceNotFoundException("Booking not found with PNR: " + nonExistentPnr));

        mockMvc.perform(get("/api/v1/bookings/{pnr}", nonExistentPnr))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Booking not found with PNR: " + nonExistentPnr));
    }

    @Test
    @DisplayName("POST /api/v1/bookings/{pnr}/cancel returns 200 OK on successful cancellation")
    void cancelBooking_Success_Returns200() throws Exception {
        String pnr = "ALG-2026-X8K2";
        BookingResponse mockResponse = new BookingResponse();
        mockResponse.setPnr(pnr);
        mockResponse.setStatus("CANCELLED");

        when(bookingService.cancelBooking(pnr)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/bookings/{pnr}/cancel", pnr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value(pnr))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private CreateBookingRequest createValidBookingRequest(UUID tripId) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setTripId(tripId);
        request.setTravelDate(LocalDate.now().plusDays(1));
        request.setBoardingStop("Dadar, Mumbai");
        request.setDropStop("Swargate, Pune");
        request.setCustomerName("Rahul Sharma");
        request.setCustomerEmail("rahul@example.com");
        request.setCustomerPhone("+919876543210");
        request.setPassengers(List.of(
                new PassengerRequest("Rahul Sharma", 28, "MALE", "1A")
        ));
        return request;
    }
}

