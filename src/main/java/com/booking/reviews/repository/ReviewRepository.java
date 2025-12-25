package com.booking.reviews.repository;

import com.booking.reviews.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("SELECT r FROM Review r WHERE r.roomId = :roomId")
    Page<Review> findByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.roomId = :roomId")
    Double findAverageRatingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.roomId = :roomId")
    Long countByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.roomId = :roomId AND r.rating = :rating")
    Long countByRoomIdAndRating(@Param("roomId") Long roomId, @Param("rating") Short rating);
}
