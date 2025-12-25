package com.booking.reviews.service;

import com.booking.reviews.dto.ConfigResponse;
import com.booking.reviews.entity.Hotel;
import com.booking.reviews.entity.HotelType;
import com.booking.reviews.exception.ResourceNotFoundException;
import com.booking.reviews.repository.HotelRepository;
import com.booking.reviews.repository.HotelTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private final HotelRepository hotelRepository;
    private final HotelTypeRepository hotelTypeRepository;
    private final FeatureToggleService featureToggleService;

    public ConfigService(
            HotelRepository hotelRepository,
            HotelTypeRepository hotelTypeRepository,
            FeatureToggleService featureToggleService) {
        this.hotelRepository = hotelRepository;
        this.hotelTypeRepository = hotelTypeRepository;
        this.featureToggleService = featureToggleService;
    }

    @Transactional(readOnly = true)
    public ConfigResponse getReviewConfig(Long hotelId) {
        logger.debug("Fetching review config for hotelId: {}", hotelId);

        Hotel hotel = hotelRepository.findByHotelId(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + hotelId));

        HotelType hotelType = hotelTypeRepository.findByHotelTypeId(hotel.getHotelTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel type not found: " + hotel.getHotelTypeId()));

        boolean globalEnabled = featureToggleService.isGlobalWriteReviewEnabled();
        boolean hotelTypeEnabled = hotelType.getReviewEnabled();

        boolean enabled = globalEnabled && hotelTypeEnabled;

        String scope;
        String reason;

        if (!globalEnabled) {
            scope = "GLOBAL";
            reason = "Reviews are currently disabled globally";
        } else if (!hotelTypeEnabled) {
            scope = "HOTEL_TYPE";
            reason = "Reviews disabled for this hotel category";
        } else {
            scope = "ENABLED";
            reason = "Reviews are enabled";
        }

        return ConfigResponse.builder()
                .enabled(enabled)
                .scope(scope)
                .reason(reason)
                .build();
    }
}
