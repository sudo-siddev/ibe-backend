package com.booking.reviews.repository;

import com.booking.reviews.entity.HotelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelTypeRepository extends JpaRepository<HotelType, Long> {

    Optional<HotelType> findByHotelTypeId(Long hotelTypeId);
}
