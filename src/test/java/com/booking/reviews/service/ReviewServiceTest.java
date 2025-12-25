package com.booking.reviews.service;

import com.booking.reviews.dto.ReviewRequest;
import com.booking.reviews.entity.*;
import com.booking.reviews.exception.DuplicateReviewException;
import com.booking.reviews.exception.FeatureDisabledException;
import com.booking.reviews.exception.ResourceNotFoundException;
import com.booking.reviews.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelTypeRepository hotelTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private ReviewService reviewService;

    private Room testRoom;
    private Hotel testHotel;
    private HotelType testHotelType;
    private Booking testBooking;
    private ReviewRequest testReviewRequest;

    @BeforeEach
    void setUp() {
        testHotelType = HotelType.builder()
                .hotelTypeId(1L)
                .typeName("Luxury")
                .reviewEnabled(true)
                .build();

        testHotel = Hotel.builder()
                .hotelId(1L)
                .hotelTypeId(1L)
                .hotelName("Test Hotel")
                .build();

        testRoom = Room.builder()
                .roomId(1L)
                .hotelId(1L)
                .roomNumber("101")
                .build();

        testBooking = Booking.builder()
                .bookingId(1L)
                .roomId(1L)
                .guestEmail("guest@example.com")
                .guestName("John Doe")
                .build();

        testReviewRequest = ReviewRequest.builder()
                .roomId(1L)
                .bookingId(1L)
                .rating((short) 5)
                .comment("Great stay!")
                .reviewerEmail("guest@example.com")
                .reviewerName("John Doe")
                .build();
    }

    @Test
    void createReview_Success() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(hotelRepository.findByHotelId(1L)).thenReturn(Optional.of(testHotel));
        when(hotelTypeRepository.findByHotelTypeId(1L)).thenReturn(Optional.of(testHotelType));
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(true);

        Review savedReview = Review.builder()
                .reviewId(1L)
                .roomId(1L)
                .bookingId(1L)
                .rating((short) 5)
                .comment("Great stay!")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // Act
        var result = reviewService.createReview(testReviewRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getReviewId());
        assertEquals((short) 5, result.getRating());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void createReview_RoomNotFound() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> reviewService.createReview(testReviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_BookingNotFound() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> reviewService.createReview(testReviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_BookingRoomMismatch() {
        // Arrange
        Booking wrongRoomBooking = Booking.builder()
                .bookingId(1L)
                .roomId(999L) // Different room
                .guestEmail("guest@example.com")
                .build();

        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.of(wrongRoomBooking));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> reviewService.createReview(testReviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_EmailMismatch() {
        // Arrange
        ReviewRequest wrongEmailRequest = ReviewRequest.builder()
                .roomId(1L)
                .bookingId(1L)
                .rating((short) 5)
                .reviewerEmail("wrong@example.com") // Different email
                .build();

        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> reviewService.createReview(wrongEmailRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_DuplicateReview() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateReviewException.class, () -> reviewService.createReview(testReviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_GlobalFeatureDisabled() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(hotelRepository.findByHotelId(1L)).thenReturn(Optional.of(testHotel));
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(false);

        // Act & Assert
        assertThrows(FeatureDisabledException.class, () -> reviewService.createReview(testReviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_HotelTypeFeatureDisabled() {
        // Arrange
        HotelType disabledHotelType = HotelType.builder()
                .hotelTypeId(1L)
                .typeName("Budget")
                .reviewEnabled(false)
                .build();

        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.of(testBooking));
        when(reviewRepository.existsByBookingId(1L)).thenReturn(false);
        when(hotelRepository.findByHotelId(1L)).thenReturn(Optional.of(testHotel));
        when(hotelTypeRepository.findByHotelTypeId(1L)).thenReturn(Optional.of(disabledHotelType));
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(true);

        // Act & Assert
        assertThrows(FeatureDisabledException.class, () -> reviewService.createReview(testReviewRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void getReviewsByRoomId_Success() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));

        Review review1 = Review.builder()
                .reviewId(1L)
                .roomId(1L)
                .bookingId(1L)
                .rating((short) 5)
                .createdAt(LocalDateTime.now())
                .build();

        Review review2 = Review.builder()
                .reviewId(2L)
                .roomId(1L)
                .bookingId(2L)
                .rating((short) 4)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        List<Review> reviews = Arrays.asList(review1, review2);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = new PageImpl<>(reviews, pageable, 2);

        when(reviewRepository.findByRoomId(1L, pageable)).thenReturn(reviewPage);
        when(bookingRepository.findByBookingId(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.findByBookingId(2L)).thenReturn(Optional.of(testBooking));

        // Act
        var result = reviewService.getReviewsByRoomId(1L, 0, 10, null);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(reviewRepository, times(1)).findByRoomId(1L, pageable);
    }

    @Test
    void getReviewsByRoomId_RoomNotFound() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewsByRoomId(1L, 0, 10, null));
    }

    @Test
    void getReviewStats_Success() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.of(testRoom));
        when(reviewRepository.countByRoomId(1L)).thenReturn(10L);
        when(reviewRepository.findAverageRatingByRoomId(1L)).thenReturn(4.5);
        when(reviewRepository.countByRoomIdAndRating(1L, (short) 1)).thenReturn(0L);
        when(reviewRepository.countByRoomIdAndRating(1L, (short) 2)).thenReturn(1L);
        when(reviewRepository.countByRoomIdAndRating(1L, (short) 3)).thenReturn(2L);
        when(reviewRepository.countByRoomIdAndRating(1L, (short) 4)).thenReturn(3L);
        when(reviewRepository.countByRoomIdAndRating(1L, (short) 5)).thenReturn(4L);

        // Act
        var result = reviewService.getReviewStats(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getRoomId());
        assertEquals(10L, result.getTotalReviews());
        assertEquals(4.5, result.getAverageRating());
        assertEquals(5, result.getRatingDistribution().size());
    }

    @Test
    void getReviewStats_RoomNotFound() {
        // Arrange
        when(roomRepository.findByRoomId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewStats(1L));
    }
}
