package com.booking.reviews.service;

import com.booking.reviews.dto.ConfigResponse;
import com.booking.reviews.entity.Hotel;
import com.booking.reviews.entity.HotelType;
import com.booking.reviews.exception.ResourceNotFoundException;
import com.booking.reviews.repository.HotelRepository;
import com.booking.reviews.repository.HotelTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelTypeRepository hotelTypeRepository;

    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private ConfigService configService;

    private Hotel testHotel;
    private HotelType testHotelType;

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
    }

    @Test
    void getReviewConfig_Enabled() {
        // Arrange
        when(hotelRepository.findByHotelId(1L)).thenReturn(Optional.of(testHotel));
        when(hotelTypeRepository.findByHotelTypeId(1L)).thenReturn(Optional.of(testHotelType));
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(true);

        // Act
        ConfigResponse result = configService.getReviewConfig(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.getEnabled());
        assertEquals("ENABLED", result.getScope());
    }

    @Test
    void getReviewConfig_GlobalDisabled() {
        // Arrange
        when(hotelRepository.findByHotelId(1L)).thenReturn(Optional.of(testHotel));
        when(hotelTypeRepository.findByHotelTypeId(1L)).thenReturn(Optional.of(testHotelType));
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(false);

        // Act
        ConfigResponse result = configService.getReviewConfig(1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.getEnabled());
        assertEquals("GLOBAL", result.getScope());
        assertTrue(result.getReason().contains("globally"));
    }

    @Test
    void getReviewConfig_HotelTypeDisabled() {
        // Arrange
        HotelType disabledHotelType = HotelType.builder()
                .hotelTypeId(1L)
                .typeName("Budget")
                .reviewEnabled(false)
                .build();

        when(hotelRepository.findByHotelId(1L)).thenReturn(Optional.of(testHotel));
        when(hotelTypeRepository.findByHotelTypeId(1L)).thenReturn(Optional.of(disabledHotelType));
        when(featureToggleService.isGlobalWriteReviewEnabled()).thenReturn(true);

        // Act
        ConfigResponse result = configService.getReviewConfig(1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.getEnabled());
        assertEquals("HOTEL_TYPE", result.getScope());
        assertTrue(result.getReason().contains("hotel category"));
    }

    @Test
    void getReviewConfig_HotelNotFound() {
        // Arrange
        when(hotelRepository.findByHotelId(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> configService.getReviewConfig(1L));
    }
}
