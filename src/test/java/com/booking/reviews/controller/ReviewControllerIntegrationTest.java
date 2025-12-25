package com.booking.reviews.controller;

import com.booking.reviews.dto.ReviewRequest;
import com.booking.reviews.entity.*;
import com.booking.reviews.repository.*;
import com.booking.reviews.service.FeatureToggleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelTypeRepository hotelTypeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @MockBean
    private FeatureToggleService featureToggleService;

    private HotelType testHotelType;
    private Hotel testHotel;
    private Room testRoom;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        // Mock feature toggle to return true for tests
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(true);

        // Clean up
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
        hotelTypeRepository.deleteAll();

        // Create test data
        testHotelType = hotelTypeRepository.save(HotelType.builder()
                .typeName("Luxury")
                .reviewEnabled(true)
                .build());

        testHotel = hotelRepository.save(Hotel.builder()
                .hotelTypeId(testHotelType.getHotelTypeId())
                .hotelName("Test Hotel")
                .build());

        testRoom = roomRepository.save(Room.builder()
                .hotelId(testHotel.getHotelId())
                .roomNumber("101")
                .build());

        testBooking = bookingRepository.save(Booking.builder()
                .roomId(testRoom.getRoomId())
                .guestEmail("guest@example.com")
                .guestName("John Doe")
                .build());
    }

    @Test
    @WithMockUser
    void createReview_Success() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .roomId(testRoom.getRoomId())
                .bookingId(testBooking.getBookingId())
                .rating((short) 5)
                .comment("Great stay!")
                .reviewerEmail("guest@example.com")
                .reviewerName("John Doe")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").exists())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @WithMockUser
    void createReview_ValidationError() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .roomId(null) // Invalid
                .bookingId(testBooking.getBookingId())
                .rating((short) 6) // Invalid - exceeds max
                .reviewerEmail("invalid-email") // Invalid
                .build();

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser
    void getReviewsByRoomId_Success() throws Exception {
        // Create a review first
        Review review = Review.builder()
                .roomId(testRoom.getRoomId())
                .bookingId(testBooking.getBookingId())
                .rating((short) 5)
                .comment("Great!")
                .build();
        reviewRepository.save(review);

        mockMvc.perform(get("/api/reviews/room/{roomId}", testRoom.getRoomId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    @WithMockUser
    void getReviewStats_Success() throws Exception {
        // Create reviews
        Review review1 = Review.builder()
                .roomId(testRoom.getRoomId())
                .bookingId(testBooking.getBookingId())
                .rating((short) 5)
                .build();
        reviewRepository.save(review1);

        mockMvc.perform(get("/api/reviews/stats/{roomId}", testRoom.getRoomId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(testRoom.getRoomId()))
                .andExpect(jsonPath("$.totalReviews").exists())
                .andExpect(jsonPath("$.averageRating").exists());
    }
}
