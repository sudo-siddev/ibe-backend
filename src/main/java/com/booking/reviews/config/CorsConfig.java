package com.booking.reviews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow multiple frontend origins (S3 and CloudFront)
        // Configure via CORS_ALLOWED_ORIGINS env var (comma-separated)
        // Example: CORS_ALLOWED_ORIGINS=http://ibe-review-frontend.s3-website-us-east-1.amazonaws.com,https://ibe-review-frontend.s3-website-us-east-1.amazonaws.com,https://d1234567890.cloudfront.net
        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        List<String> allowedOrigins;
        
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isEmpty()) {
            // Parse comma-separated origins from environment variable
            allowedOrigins = Arrays.stream(allowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList();
        } else {
            // Default to S3 website origins (CloudFront must be added via env var)
            allowedOrigins = List.of(
                "http://ibe-review-frontend.s3-website-us-east-1.amazonaws.com",
                "https://ibe-review-frontend.s3-website-us-east-1.amazonaws.com"
            );
        }
        
        // When using credentials, exact origin matching is required (no wildcards)
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Allow credentials (required for Basic Auth with CORS)
        // Note: When credentials are allowed, exact origin matching is enforced
        configuration.setAllowCredentials(true);
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allowed headers (including Authorization for Basic Auth)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Exposed headers (if frontend needs to read response headers)
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

