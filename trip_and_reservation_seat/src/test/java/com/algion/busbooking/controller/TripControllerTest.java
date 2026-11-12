package com.algion.busbooking.controller;

import com.algion.busbooking.dto.SeatLayoutResponse;
import com.algion.busbooking.dto.TripResponse;
import com.algion.busbooking.exception.GlobalExceptionHandler;
import com.algion.busbooking.service.TripService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@Import(GlobalExceptionHandler.class)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TripService tripService;

    @Test
    @DisplayName("GET /api/v1/trips returns 200 OK with matching trips")
    void getTrips_WithFilters_Returns200() throws Exception {
        UUID tripId = UUID.randomUUID();
        TripResponse trip = new TripResponse();
        trip.setId(tripId);
        trip.setOrigin("Mumbai");
        trip.setDestination("Pune");
        trip.setDepartureTime(LocalDateTime.now().plusHours(3));
        trip.setArrivalTime(LocalDateTime.now().plusHours(6));
        trip.setBaseFare(new BigDecimal("450.00"));
        trip.setBusType("AC_SLEEPER");
        trip.setTotalSeats(20);
        trip.setAvailableSeats(18L);
        trip.setStatus("SCHEDULED");

        when(tripService.searchTrips(eq("Mumbai"), eq("Pune"), any(LocalDate.class)))
                .thenReturn(List.of(trip));

        mockMvc.perform(get("/api/v1/trips")
                        .param("origin", "Mumbai")
                        .param("destination", "Pune")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin").value("Mumbai"))
                .andExpect(jsonPath("$[0].destination").value("Pune"))
                .andExpect(jsonPath("$[0].availableSeats").value(18));
    }

    @Test
    @DisplayName("GET /api/v1/trips/{id}/seats returns 200 OK with seat layout")
    void getSeatLayout_Returns200() throws Exception {
        UUID tripId = UUID.randomUUID();
        SeatLayoutResponse seat1 = new SeatLayoutResponse(tripId, "1A", "WINDOW", "LOWER", new BigDecimal("500.00"), "AVAILABLE");
        SeatLayoutResponse seat2 = new SeatLayoutResponse(tripId, "1B", "AISLE", "LOWER", new BigDecimal("450.00"), "BOOKED");

        when(tripService.getSeatLayout(tripId)).thenReturn(List.of(seat1, seat2));

        mockMvc.perform(get("/api/v1/trips/{id}/seats", tripId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].seatNumber").value("1A"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].seatNumber").value("1B"))
                .andExpect(jsonPath("$[1].status").value("BOOKED"));
    }
}

