package com.algion.busbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI busBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ALGIONS Bus Booking Platform API")
                        .description("Trip & Seat Reservation Microservice with Pessimistic Row-Level Locking (PESSIMISTIC_WRITE)")
                        .version("1.0.0")
                        .contact(new Contact().name("ALGIONS Cloud Engineering Team")));
    }
}
