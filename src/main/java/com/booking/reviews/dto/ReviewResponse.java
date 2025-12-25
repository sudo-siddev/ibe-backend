package com.booking.reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Review response containing review details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    @Schema(description = "Unique review ID", example = "1")
    private Long reviewId;

    @Schema(description = "Room ID", example = "1")
    private Long roomId;

    @Schema(description = "Booking ID", example = "1")
    private Long bookingId;

    @Schema(description = "Rating from 1 to 5", example = "5", minimum = "1", maximum = "5")
    private Short rating;

    @Schema(description = "Review comment", example = "Great stay! Very comfortable room.")
    private String comment;

    @Schema(description = "Reviewer email address", example = "guest@example.com")
    private String reviewerEmail;

    @Schema(description = "Reviewer name", example = "John Doe")
    private String reviewerName;

    @Schema(description = "Review creation timestamp", example = "2025-12-25T17:20:00")
    private LocalDateTime createdAt;
}
