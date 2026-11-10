package com.algion.busbooking.service;

import com.algion.busbooking.dto.RouteResponse;
import com.algion.busbooking.entity.Route;
import com.algion.busbooking.exception.ResourceNotFoundException;
import com.algion.busbooking.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<RouteResponse> getActiveRoutes() {
        return routeRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RouteResponse getRouteById(UUID id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with ID: " + id));
        return mapToResponse(route);
    }

    private RouteResponse mapToResponse(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getOrigin(),
                route.getDestination(),
                route.getDistanceKm(),
                route.getEstimatedMinutes()
        );
    }
}

