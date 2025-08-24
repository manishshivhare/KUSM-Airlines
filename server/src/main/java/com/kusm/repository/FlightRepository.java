package com.kusm.repository;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kusm.model.Flight;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    
    @Query("SELECT f FROM Flight f WHERE f.origin = :origin AND f.destination = :destination AND DATE(f.departureTime) = DATE(:departureDate) AND f.availableSeats > 0")
    List<Flight> findAvailableFlights(@Param("origin") String origin, 
                                    @Param("destination") String destination, 
                                    @Param("departureDate") LocalDateTime departureDate);
    
    List<Flight> findByOriginAndDestination(String origin, String destination);
    
    @Query("SELECT DISTINCT f.origin FROM Flight f")
    List<String> findAllOrigins();
    
    @Query("SELECT DISTINCT f.destination FROM Flight f")
    List<String> findAllDestinations();
}