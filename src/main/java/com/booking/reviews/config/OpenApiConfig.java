package com.booking.reviews.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reviewRatingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Review & Rating System API")
                        .description("""
                                Backend API for Review & Rating System - A standalone component of a multi-tenant Internet Booking Engine (IBE).
                                
                                ## Features
                                - Create and manage guest reviews
                                - Get reviews by room with pagination
                                - Review statistics and rating distribution
                                - Feature toggle configuration
                                
                                ## Security
                                - All endpoints except `/health` require Basic Authentication
                                - Use the **Authorize** button to set credentials
                                
                                ## Feature Toggles
                                - Global kill switch via AWS Parameter Store
                                - Hotel-type level control via database
                                - Reviews enabled only when both are true
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Review Rating Backend")
                                .email("support@booking.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://booking.com")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("Basic Authentication")));
    }
}


