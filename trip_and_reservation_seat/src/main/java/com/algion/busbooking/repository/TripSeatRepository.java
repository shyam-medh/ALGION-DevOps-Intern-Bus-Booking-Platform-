package com.algion.busbooking.repository;

import com.algion.busbooking.entity.TripSeat;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripSeatRepository extends JpaRepository<TripSeat, UUID> {

    List<TripSeat> findByTripIdOrderBySeatSeatNumberAsc(UUID tripId);

    List<TripSeat> findByTripIdAndStatus(UUID tripId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT ts FROM TripSeat ts " +
           "WHERE ts.trip.id = :tripId AND ts.seat.seatNumber = :seatNumber")
    Optional<TripSeat> findByTripIdAndSeatNumberWithLock(
            @Param("tripId") UUID tripId,
            @Param("seatNumber") String seatNumber
    );

    Optional<TripSeat> findByTripIdAndSeatSeatNumber(UUID tripId, String seatNumber);
}
