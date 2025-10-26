package com.mini.soccer.repository;

import com.mini.soccer.model.Booking;
import com.mini.soccer.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByBookingCode(String bookingCode);

    List<Booking> findAllByOrderByCreatedAtDesc();

    List<Booking> findByBookingCodeContainingIgnoreCaseOrderByCreatedAtDesc(String bookingCode);

    @Query("""
        select case when count(b) > 0 then true else false end
        from Booking b
        where b.field.fieldId = :fieldId
          and b.status in :activeStatuses
          and b.startTime < :requestedEnd
          and b.endTime > :requestedStart
        """)
    boolean existsOverlappingBooking(@Param("fieldId") Long fieldId,
                                     @Param("activeStatuses") Collection<BookingStatus> activeStatuses,
                                     @Param("requestedStart") LocalDateTime requestedStart,
                                     @Param("requestedEnd") LocalDateTime requestedEnd);
}
