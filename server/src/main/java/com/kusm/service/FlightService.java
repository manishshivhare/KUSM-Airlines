package com.kusm.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusm.model.Flight;
import com.kusm.repository.FlightRepository;

@Service
public class FlightService {
    
    @Autowired
    private FlightRepository flightRepository;
    
    @Autowired
    private SeatService seatService;
    
    public List<Flight> searchFlights(String origin, String destination, LocalDateTime departureDate) {
        return flightRepository.findAvailableFlights(origin, destination, departureDate);
    }
    
    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }
    
    public Optional<Flight> getFlightById(Long id) {
        return flightRepository.findById(id);
    }
    
    @Transactional
    public Flight saveFlight(Flight flight) {
        Flight savedFlight = flightRepository.save(flight);
        
        // Initialize seats when a new flight is created
        if (flight.getId() == null) { // New flight
            seatService.initializeSeatsForFlight(savedFlight);
        }
        
        return savedFlight;
    }
    
    public List<String> getAllOrigins() {
        return flightRepository.findAllOrigins();
    }
    
    public List<String> getAllDestinations() {
        return flightRepository.findAllDestinations();
    }
    
    @Transactional
    public boolean updateAvailableSeats(Long flightId, int seatsToBook) {
        Optional<Flight> flightOpt = flightRepository.findById(flightId);
        if (flightOpt.isPresent()) {
            Flight flight = flightOpt.get();
            
            // Get actual available seat count from seat service
            long actualAvailableSeats = seatService.getAvailableSeatCount(flightId);
            
            if (actualAvailableSeats >= seatsToBook) {
                // Update the flight's available seats to match actual count
                flight.setAvailableSeats((int) (actualAvailableSeats - seatsToBook));
                flightRepository.save(flight);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Synchronize flight's available seats with actual seat availability
     */
    @Transactional
    public void synchronizeAvailableSeats(Long flightId) {
        Optional<Flight> flightOpt = flightRepository.findById(flightId);
        if (flightOpt.isPresent()) {
            Flight flight = flightOpt.get();
            long actualAvailableSeats = seatService.getAvailableSeatCount(flightId);
            flight.setAvailableSeats((int) actualAvailableSeats);
            flightRepository.save(flight);
        }
    }
    
    /**
     * Get flight with real-time seat availability
     */
    public Optional<Flight> getFlightWithRealTimeAvailability(Long flightId) {
        Optional<Flight> flightOpt = flightRepository.findById(flightId);
        if (flightOpt.isPresent()) {
            Flight flight = flightOpt.get();
            long actualAvailableSeats = seatService.getAvailableSeatCount(flightId);
            flight.setAvailableSeats((int) actualAvailableSeats);
            return Optional.of(flight);
        }
        return Optional.empty();
    }
}