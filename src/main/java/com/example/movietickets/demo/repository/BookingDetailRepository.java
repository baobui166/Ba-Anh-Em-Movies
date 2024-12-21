package com.example.movietickets.demo.repository;

import com.example.movietickets.demo.model.Booking;
import com.example.movietickets.demo.model.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail, Long> {
    List<BookingDetail> findByScheduleId(Long scheduleId);
    List<Booking> findByBookingId(Long bookingId);
//    @Query("SELECT bd FROM BookingDetail bd WHERE bd.schedule.id = :scheduleId")
//    List<BookingDetail> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT bd FROM BookingDetail bd WHERE bd.seat.id = :seatId AND bd.schedule.id = :scheduleId")
    Optional<BookingDetail> findBySeatAndSchedule(@Param("seatId") Long seatId, @Param("scheduleId") Long scheduleId);

    List<BookingDetail> findBookingDetailByScheduleId(Long scheduleId);

    @Query("SELECT bd FROM BookingDetail bd WHERE bd.seat.room.id = :roomId AND bd.schedule.id = :scheduleId")
    List<BookingDetail> findByRoomIdAndScheduleId(
            @Param("roomId") Long roomId,
            @Param("scheduleId") Long scheduleId
    );
}
