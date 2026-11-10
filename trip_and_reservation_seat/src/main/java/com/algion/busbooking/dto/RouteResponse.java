package com.algion.busbooking.dto;

import java.util.UUID;

public class RouteResponse {

    private UUID id;
    private String origin;
    private String destination;
    private Double distanceKm;
    private Integer estimatedMinutes;

    public RouteResponse() {
    }

    public RouteResponse(UUID id, String origin, String destination, Double distanceKm, Integer estimatedMinutes) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.estimatedMinutes = estimatedMinutes;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }
}

