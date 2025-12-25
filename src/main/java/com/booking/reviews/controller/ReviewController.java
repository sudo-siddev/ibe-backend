package com.booking.reviews.controller;

import com.booking.reviews.dto.ErrorResponse;
import com.booking.reviews.dto.ReviewRequest;
import com.booking.reviews.dto.ReviewResponse;
import com.booking.reviews.dto.ReviewStatsResponse;
import com.booking.reviews.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reviews", description = "Review management APIs - Create, retrieve, and get statistics for reviews")
@RestController
@RequestMapping("/api/reviews")
@SecurityRequirement(name = "basicAuth")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(
            summary = "Create a new review",
            description = """
                    Creates a new review for a booking. 
                    
                    **Business Rules:**
                    - One review per booking (enforced by unique constraint)
                    - Rating must be between 1-5
                    - Reviewer email must match booking guest email
                    - Booking must belong to the specified room
                    - Feature toggle must be enabled (global + hotel type)
                    
                    **Validation:**
                    - Room ID: Required, must be positive
                    - Booking ID: Required, must be positive
                    - Rating: Required, must be 1-5
                    - Comment: Optional, max 1000 characters
                    - Reviewer Email: Required, valid email format
                    - Reviewer Name: Optional
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Review created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReviewResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                                "reviewId": 1,
                                                "roomId": 1,
                                                "bookingId": 1,
                                                "rating": 5,
                                                "comment": "Great stay!",
                                                "reviewerEmail": "guest@example.com",
                                                "reviewerName": "John Doe",
                                                "createdAt": "2025-12-25T17:20:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Validation Error",
                                    value = """
                                            {
                                                "code": "VALIDATION_ERROR",
                                                "message": "Validation failed: Rating must be at most 5",
                                                "timestamp": "2025-12-25T17:20:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Feature disabled",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Feature Disabled",
                                    value = """
                                            {
                                                "code": "FEATURE_DISABLED",
                                                "message": "Reviews are disabled for this hotel type",
                                                "timestamp": "2025-12-25T17:20:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found (room, booking, or hotel)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Not Found",
                                    value = """
                                            {
                                                "code": "RESOURCE_NOT_FOUND",
                                                "message": "Room not found: 999",
                                                "timestamp": "2025-12-25T17:20:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate review (review already exists for this booking)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Conflict",
                                    value = """
                                            {
                                                "code": "DUPLICATE_REVIEW",
                                                "message": "A review already exists for booking: 1",
                                                "timestamp": "2025-12-25T17:20:00"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        logger.info("POST /api/reviews - Creating review for roomId: {}", request.getRoomId());
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get reviews by room ID",
            description = """
                    Retrieves paginated reviews for a specific room, sorted by creation date (newest first by default).
                    
                    **Pagination:**
                    - page: Page number (0-indexed, default: 0)
                    - size: Page size (default: 10)
                    - sortBy: Sort field and direction (format: "field,direction", e.g., "createdAt,desc" or "rating,asc")
                    
                    **Default Sort:** Created date descending (newest first)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reviews retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByRoomId(
            @Parameter(description = "Room ID", example = "1", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field and direction (format: field,direction)", example = "createdAt,desc")
            @RequestParam(required = false) String sortBy) {
        logger.info("GET /api/reviews/room/{} - page: {}, size: {}, sortBy: {}", roomId, page, size, sortBy);
        Page<ReviewResponse> response = reviewService.getReviewsByRoomId(roomId, page, size, sortBy);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get review statistics for a room",
            description = """
                    Returns aggregated statistics for reviews of a specific room.
                    
                    **Statistics Include:**
                    - Total number of reviews
                    - Average rating (rounded to 2 decimal places)
                    - Rating distribution (count for each rating 1-5)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ReviewStatsResponse.class),
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                                "roomId": 1,
                                                "totalReviews": 10,
                                                "averageRating": 4.5,
                                                "ratingDistribution": {
                                                    "1": 0,
                                                    "2": 1,
                                                    "3": 2,
                                                    "4": 3,
                                                    "5": 4
                                                }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/stats/{roomId}")
    public ResponseEntity<ReviewStatsResponse> getReviewStats(
            @Parameter(description = "Room ID", example = "1", required = true)
            @PathVariable Long roomId) {
        logger.info("GET /api/reviews/stats/{}", roomId);
        ReviewStatsResponse response = reviewService.getReviewStats(roomId);
        return ResponseEntity.ok(response);
    }
}

