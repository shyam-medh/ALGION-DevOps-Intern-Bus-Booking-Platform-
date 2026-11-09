package com.algion.busbooking.repository;

import com.algion.busbooking.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

    List<Route> findByActiveTrue();

    Optional<Route> findByOriginIgnoreCaseAndDestinationIgnoreCase(String origin, String destination);
}
