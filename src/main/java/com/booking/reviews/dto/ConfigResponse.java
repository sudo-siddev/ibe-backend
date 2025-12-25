package com.booking.reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Review configuration status for a hotel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigResponse {

    @Schema(description = "Whether reviews are enabled for this hotel", example = "true")
    private Boolean enabled;

    @Schema(description = "Scope of the configuration (GLOBAL, HOTEL_TYPE, or ENABLED)", 
            example = "ENABLED", 
            allowableValues = {"GLOBAL", "HOTEL_TYPE", "ENABLED"})
    private String scope;

    @Schema(description = "Reason for the current status", example = "Reviews are enabled")
    private String reason;
}

