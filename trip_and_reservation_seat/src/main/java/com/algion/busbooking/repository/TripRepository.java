package com.algion.busbooking.repository;

import com.algion.busbooking.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByRouteIdAndDepartureTimeBetween(UUID routeId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Trip t " +
           "WHERE LOWER(t.route.origin) = LOWER(:origin) " +
           "AND LOWER(t.route.destination) = LOWER(:destination) " +
           "AND t.departureTime BETWEEN :startOfDay AND :endOfDay " +
           "AND t.status = 'SCHEDULED' " +
           "ORDER BY t.departureTime ASC")
    List<Trip> searchTrips(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
