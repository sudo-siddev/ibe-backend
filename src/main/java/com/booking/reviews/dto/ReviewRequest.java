package com.booking.reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request to create a new review")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @Schema(description = "Room ID where the review is for", example = "1", required = true)
    @NotNull(message = "Room ID is required")
    @Positive(message = "Room ID must be positive")
    private Long roomId;

    @Schema(description = "Booking ID associated with this review (one review per booking)", example = "1", required = true)
    @NotNull(message = "Booking ID is required")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;

    @Schema(description = "Rating from 1 to 5", example = "5", minimum = "1", maximum = "5", required = true)
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Short rating;

    @Schema(description = "Optional review comment (max 1000 characters)", example = "Great stay! Very comfortable room.", maxLength = 1000)
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    @Schema(description = "Reviewer email address (must match booking guest email)", example = "guest@example.com", required = true)
    @NotBlank(message = "Reviewer email is required")
    @Email(message = "Reviewer email must be valid")
    private String reviewerEmail;

    @Schema(description = "Optional reviewer name", example = "John Doe")
    private String reviewerName;
}

