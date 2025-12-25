package com.booking.reviews.service;

import com.booking.reviews.dto.ReviewRequest;
import com.booking.reviews.dto.ReviewResponse;
import com.booking.reviews.dto.ReviewStatsResponse;
import com.booking.reviews.entity.*;
import com.booking.reviews.exception.DuplicateReviewException;
import com.booking.reviews.exception.FeatureDisabledException;
import com.booking.reviews.exception.ResourceNotFoundException;
import com.booking.reviews.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final HotelTypeRepository hotelTypeRepository;
    private final BookingRepository bookingRepository;
    private final FeatureToggleService featureToggleService;

    public ReviewService(
            ReviewRepository reviewRepository,
            RoomRepository roomRepository,
            HotelRepository hotelRepository,
            HotelTypeRepository hotelTypeRepository,
            BookingRepository bookingRepository,
            FeatureToggleService featureToggleService) {
        this.reviewRepository = reviewRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.hotelTypeRepository = hotelTypeRepository;
        this.bookingRepository = bookingRepository;
        this.featureToggleService = featureToggleService;
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        logger.info("Creating review for roomId: {}, bookingId: {}", request.getRoomId(), request.getBookingId());

        Room room = roomRepository.findByRoomId(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));

        Booking booking = bookingRepository.findByBookingId(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

        if (!booking.getRoomId().equals(request.getRoomId())) {
            throw new ResourceNotFoundException("Booking does not belong to the specified room");
        }

        if (!booking.getGuestEmail().equalsIgnoreCase(request.getReviewerEmail())) {
            throw new ResourceNotFoundException("Reviewer email does not match booking guest email");
        }

        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new DuplicateReviewException("A review already exists for booking: " + request.getBookingId());
        }

        Hotel hotel = hotelRepository.findByHotelId(room.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + room.getHotelId()));

        checkWriteReviewEnabled(hotel.getHotelTypeId());

        Review review = Review.builder()
                .roomId(request.getRoomId())
                .bookingId(request.getBookingId())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);
        logger.info("Review created successfully with id: {}", savedReview.getReviewId());

        return mapToResponse(savedReview, booking);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByRoomId(Long roomId, int page, int size, String sortBy) {
        logger.debug("Fetching reviews for roomId: {}, page: {}, size: {}, sortBy: {}", roomId, page, size, sortBy);

        roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortBy != null && !sortBy.isEmpty()) {
            try {
                String[] sortParts = sortBy.split(",");
                if (sortParts.length == 2) {
                    String field = sortParts[0].trim();
                    String directionStr = sortParts[1].trim();
                    
                    if (!field.equals("createdAt") && !field.equals("rating")) {
                        logger.warn("Invalid sort field: {}, allowed fields are 'createdAt' or 'rating'. Using default sort.", field);
                    } else {
                        Sort.Direction direction = directionStr.equalsIgnoreCase("asc") 
                                ? Sort.Direction.ASC 
                                : Sort.Direction.DESC;
                        
                        Sort.Order primaryOrder = new Sort.Order(direction, field);
                        
                        if (field.equals("rating")) {
                            Sort.Order secondaryOrder = new Sort.Order(Sort.Direction.DESC, "createdAt");
                            sort = Sort.by(primaryOrder, secondaryOrder);
                        } else {
                            sort = Sort.by(primaryOrder);
                        }
                    }
                } else {
                    logger.warn("Invalid sortBy format: {}. Expected format: 'field,direction'. Using default sort.", sortBy);
                }
            } catch (Exception e) {
                logger.warn("Invalid sort parameter: {}, using default sort. Error: {}", sortBy, e.getMessage());
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Review> reviews = reviewRepository.findByRoomId(roomId, pageable);

        return reviews.map(review -> {
            Booking booking = bookingRepository.findByBookingId(review.getBookingId())
                    .orElse(null);
            return mapToResponse(review, booking);
        });
    }

    @Transactional(readOnly = true)
    public ReviewStatsResponse getReviewStats(Long roomId) {
        logger.debug("Fetching review stats for roomId: {}", roomId);

        roomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        Long totalReviews = reviewRepository.countByRoomId(roomId);
        Double averageRating = reviewRepository.findAverageRatingByRoomId(roomId);

        Map<Short, Long> ratingDistribution = new HashMap<>();
        for (short rating = 1; rating <= 5; rating++) {
            Long count = reviewRepository.countByRoomIdAndRating(roomId, rating);
            ratingDistribution.put(rating, count);
        }

        return ReviewStatsResponse.builder()
                .roomId(roomId)
                .totalReviews(totalReviews)
                .averageRating(averageRating != null ? Math.round(averageRating * 100.0) / 100.0 : null)
                .ratingDistribution(ratingDistribution)
                .build();
    }

    private void checkWriteReviewEnabled(Long hotelTypeId) {
        boolean globalEnabled = featureToggleService.isGlobalWriteReviewEnabled();
        if (!globalEnabled) {
            logger.warn("Write review feature is globally disabled");
            throw new FeatureDisabledException("Reviews are currently disabled globally");
        }

        HotelType hotelType = hotelTypeRepository.findByHotelTypeId(hotelTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel type not found: " + hotelTypeId));

        if (!hotelType.getReviewEnabled()) {
            logger.warn("Write review feature is disabled for hotel type: {}", hotelTypeId);
            throw new FeatureDisabledException("Reviews are disabled for this hotel type");
        }
    }

    private ReviewResponse mapToResponse(Review review, Booking booking) {
        ReviewResponse.ReviewResponseBuilder builder = ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .roomId(review.getRoomId())
                .bookingId(review.getBookingId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt());

        if (booking != null) {
            builder.reviewerEmail(booking.getGuestEmail())
                   .reviewerName(booking.getGuestName());
        }

        return builder.build();
    }
}
