package com.algion.busbooking.repository;

import com.algion.busbooking.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusRepository extends JpaRepository<Bus, UUID> {

    Optional<Bus> findByRegistrationNumber(String registrationNumber);
}
