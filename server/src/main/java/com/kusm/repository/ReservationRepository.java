// Add these methods to your ReservationRepository interface

package com.kusm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kusm.model.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    // Existing methods...
    Optional<Reservation> findByBookingReference(String bookingReference);
    List<Reservation> findByPassengerEmail(String email);
    
    // NEW METHODS - Add these to eagerly load seats
    
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.seats WHERE r.id = :id")
    Optional<Reservation> findByIdWithSeats(@Param("id") Long id);
    
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.seats WHERE r.bookingReference = :bookingReference")
    Optional<Reservation> findByBookingReferenceWithSeats(@Param("bookingReference") String bookingReference);
    
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.seats WHERE r.passengerEmail = :email")
    List<Reservation> findByPassengerEmailWithSeats(@Param("email") String email);
    
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.seats")
    List<Reservation> findAllWithSeats();
}