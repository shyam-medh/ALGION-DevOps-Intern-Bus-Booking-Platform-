package com.algion.busbooking.repository;

import com.algion.busbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByBusIdOrderBySeatNumberAsc(UUID busId);

    Optional<Seat> findByBusIdAndSeatNumber(UUID busId, String seatNumber);
}
