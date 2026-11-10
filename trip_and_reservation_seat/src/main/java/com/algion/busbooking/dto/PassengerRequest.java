package com.algion.busbooking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PassengerRequest {

    @NotBlank(message = "Passenger full name is required")
    private String fullName;

    @NotNull(message = "Passenger age is required")
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must be valid")
    private Integer age;

    @NotBlank(message = "Gender is required (MALE, FEMALE, OTHER)")
    private String gender;

    @NotBlank(message = "Seat number is required")
    private String seatNumber;

    public PassengerRequest() {
    }

    public PassengerRequest(String fullName, Integer age, String gender, String seatNumber) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.seatNumber = seatNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }
}

