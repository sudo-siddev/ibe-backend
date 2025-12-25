package com.booking.reviews.controller;

import com.booking.reviews.dto.ConfigResponse;
import com.booking.reviews.dto.ErrorResponse;
import com.booking.reviews.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Configuration", description = "Review feature configuration APIs")
@RestController
@RequestMapping("/api/config")
@SecurityRequirement(name = "basicAuth")
public class ConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @Operation(
            summary = "Get review configuration for a hotel",
            description = """
                    Returns the review feature configuration status for a specific hotel.
                    
                    **Feature Toggle Logic:**
                    - Reviews are enabled only when BOTH conditions are true:
                      1. Global toggle (AWS Parameter Store) is enabled
                      2. Hotel type has review_enabled = true
                    
                    **Response Scopes:**
                    - `GLOBAL`: Global feature toggle is disabled
                    - `HOTEL_TYPE`: Hotel type has reviews disabled
                    - `ENABLED`: Reviews are enabled for this hotel
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Configuration retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConfigResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Enabled",
                                            value = """
                                                    {
                                                        "enabled": true,
                                                        "scope": "ENABLED",
                                                        "reason": "Reviews are enabled"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Global Disabled",
                                            value = """
                                                    {
                                                        "enabled": false,
                                                        "scope": "GLOBAL",
                                                        "reason": "Reviews are currently disabled globally"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Hotel Type Disabled",
                                            value = """
                                                    {
                                                        "enabled": false,
                                                        "scope": "HOTEL_TYPE",
                                                        "reason": "Reviews disabled for this hotel category"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid hotelId parameter",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                                "code": "VALIDATION_ERROR",
                                                "message": "hotelId parameter is required and must be a positive number",
                                                "timestamp": "2025-12-25T17:20:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Hotel not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                            {
                                                "code": "RESOURCE_NOT_FOUND",
                                                "message": "Hotel not found: 999",
                                                "timestamp": "2025-12-25T17:20:00"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/reviews")
    public ResponseEntity<?> getReviewConfig(
            @Parameter(description = "Hotel ID", example = "1", required = true)
            @RequestParam(required = true, name = "hotelId") String hotelIdParam) {
        logger.info("GET /api/config/reviews?hotelId={}", hotelIdParam);
        
        if (hotelIdParam == null || hotelIdParam.trim().isEmpty()) {
            logger.warn("hotelId parameter is missing or empty");
            ErrorResponse error = ErrorResponse.of("VALIDATION_ERROR", "hotelId parameter is required and must be a positive number");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        Long hotelId;
        try {
            hotelId = Long.parseLong(hotelIdParam.trim());
            if (hotelId <= 0) {
                logger.warn("hotelId must be positive, received: {}", hotelId);
                ErrorResponse error = ErrorResponse.of("VALIDATION_ERROR", "hotelId must be a positive number");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid hotelId format: {}", hotelIdParam);
            ErrorResponse error = ErrorResponse.of("VALIDATION_ERROR", "hotelId must be a valid number");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        ConfigResponse response = configService.getReviewConfig(hotelId);
        return ResponseEntity.ok(response);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        logger.warn("Missing required parameter: {}", ex.getParameterName());
        ErrorResponse error = ErrorResponse.of("VALIDATION_ERROR", 
            String.format("Required parameter '%s' is missing", ex.getParameterName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

