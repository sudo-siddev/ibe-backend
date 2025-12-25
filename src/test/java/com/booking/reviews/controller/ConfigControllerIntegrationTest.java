package com.booking.reviews.controller;

import com.booking.reviews.entity.*;
import com.booking.reviews.repository.*;
import com.booking.reviews.service.FeatureToggleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class ConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelTypeRepository hotelTypeRepository;

    @MockBean
    private FeatureToggleService featureToggleService;

    private HotelType testHotelType;
    private Hotel testHotel;

    @BeforeEach
    void setUp() {
        // Mock feature toggle to return true for tests
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(true);

        hotelRepository.deleteAll();
        hotelTypeRepository.deleteAll();

        testHotelType = hotelTypeRepository.save(HotelType.builder()
                .typeName("Luxury")
                .reviewEnabled(true)
                .build());

        testHotel = hotelRepository.save(Hotel.builder()
                .hotelTypeId(testHotelType.getHotelTypeId())
                .hotelName("Test Hotel")
                .build());
    }

    @Test
    @WithMockUser
    void getReviewConfig_Success() throws Exception {
        mockMvc.perform(get("/api/config/reviews")
                        .param("hotelId", String.valueOf(testHotel.getHotelId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").exists())
                .andExpect(jsonPath("$.scope").exists())
                .andExpect(jsonPath("$.reason").exists());
    }

    @Test
    @WithMockUser
    void getReviewConfig_HotelNotFound() throws Exception {
        mockMvc.perform(get("/api/config/reviews")
                        .param("hotelId", "99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
