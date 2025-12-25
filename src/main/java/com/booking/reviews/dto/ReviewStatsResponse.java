package com.booking.reviews.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Schema(description = "Review statistics for a room")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {

    @Schema(description = "Room ID", example = "1")
    private Long roomId;

    @Schema(description = "Total number of reviews for this room", example = "10")
    private Long totalReviews;

    @Schema(description = "Average rating (rounded to 2 decimal places)", example = "4.5")
    private Double averageRating;

    @Schema(description = "Rating distribution map (rating -> count)", example = "{\"1\": 0, \"2\": 1, \"3\": 2, \"4\": 3, \"5\": 4}")
    private Map<Short, Long> ratingDistribution;
}

