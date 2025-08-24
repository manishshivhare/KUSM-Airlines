package com.kusm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kusm.model.Seat;
import com.kusm.model.Seat.SeatClass;
import com.kusm.model.Seat.SeatStatus;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    
    List<Seat> findByFlightIdAndStatus(Long flightId, SeatStatus status);
    
    List<Seat> findByFlightId(Long flightId);
    
    List<Seat> findByFlightIdAndSeatClass(Long flightId, SeatClass seatClass);
    
    List<Seat> findByFlightIdAndSeatClassAndStatus(Long flightId, SeatClass seatClass, SeatStatus status);
    
    Optional<Seat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);
    
    List<Seat> findByReservationId(Long reservationId);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.flight.id = :flightId AND s.status = :status")
    long countByFlightIdAndStatus(@Param("flightId") Long flightId, @Param("status") SeatStatus status);
    
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.flight.id = :flightId AND s.seatClass = :seatClass AND s.status = :status")
    long countByFlightIdAndSeatClassAndStatus(@Param("flightId") Long flightId, 
                                              @Param("seatClass") SeatClass seatClass, 
                                              @Param("status") SeatStatus status);
    
    @Query("SELECT s FROM Seat s WHERE s.flight.id = :flightId AND s.status = 'AVAILABLE' " +
           "ORDER BY s.seatClass, s.seatNumber")
    List<Seat> findAvailableSeatsByFlightIdOrderBySeatNumber(@Param("flightId") Long flightId);
    
    @Query("SELECT s FROM Seat s WHERE s.flight.id = :flightId AND s.seatClass = :seatClass " +
           "AND s.status = 'AVAILABLE' ORDER BY s.seatNumber")
    List<Seat> findAvailableSeatsByFlightIdAndSeatClassOrderBySeatNumber(@Param("flightId") Long flightId, 
                                                                         @Param("seatClass") SeatClass seatClass);
}