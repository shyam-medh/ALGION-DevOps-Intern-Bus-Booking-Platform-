package com.algion.busbooking.repository;

import com.algion.busbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByPnr(String pnr);

    boolean existsByPnr(String pnr);
}
