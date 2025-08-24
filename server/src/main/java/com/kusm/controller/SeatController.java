package com.kusm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kusm.model.Seat;
import com.kusm.model.Seat.SeatClass;
import com.kusm.service.ReservationService;
import com.kusm.service.SeatService;

@RestController
@RequestMapping("/api/seats")
public class SeatController {
    
    @Autowired
    private SeatService seatService;
    
    @Autowired
    private ReservationService reservationService;
    
    /**
     * Get available seats for a flight
     */
    @GetMapping("/flight/{flightId}/available")
    public ResponseEntity<List<Seat>> getAvailableSeats(@PathVariable Long flightId) {
        try {
            List<Seat> availableSeats = seatService.getAvailableSeats(flightId);
            return ResponseEntity.ok(availableSeats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get available seats by class
     */
    @GetMapping("/flight/{flightId}/available/{seatClass}")
    public ResponseEntity<List<Seat>> getAvailableSeatsByClass(
            @PathVariable Long flightId,
            @PathVariable SeatClass seatClass) {
        try {
            List<Seat> availableSeats = seatService.getAvailableSeatsByClass(flightId, seatClass);
            return ResponseEntity.ok(availableSeats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get seat map for a flight
     */
    @GetMapping("/flight/{flightId}/seatmap")
    public ResponseEntity<List<List<Seat>>> getSeatMap(@PathVariable Long flightId) {
        try {
            List<List<Seat>> seatMap = seatService.getSeatMap(flightId);
            return ResponseEntity.ok(seatMap);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get all seats for a flight
     */
    @GetMapping("/flight/{flightId}")
    public ResponseEntity<List<Seat>> getAllSeatsForFlight(@PathVariable Long flightId) {
        try {
            List<Seat> seats = seatService.getAllSeatsForFlight(flightId);
            return ResponseEntity.ok(seats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Initialize seats for a flight
     */
    @PostMapping("/flight/{flightId}/initialize")
    public ResponseEntity<String> initializeSeats(@PathVariable Long flightId) {
        try {
            // This will be called through FlightService when a flight is created
            return ResponseEntity.ok("Seats will be initialized when first reservation is made");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to initialize seats: " + e.getMessage());
        }
    }
    
    /**
     * Change seat for a reservation
     */
    @PutMapping("/reservation/{bookingReference}/change-seat")
    public ResponseEntity<String> changeSeat(
            @PathVariable String bookingReference,
            @RequestParam String newSeatNumber) {
        try {
            boolean success = reservationService.changeSeat(bookingReference, newSeatNumber);
            if (success) {
                return ResponseEntity.ok("Seat changed successfully to " + newSeatNumber);
            } else {
                return ResponseEntity.badRequest().body("Failed to change seat");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error changing seat: " + e.getMessage());
        }
    }
    
    /**
     * Get seat statistics for a flight
     */
    @GetMapping("/flight/{flightId}/statistics")
    public ResponseEntity<SeatStatistics> getSeatStatistics(@PathVariable Long flightId) {
        try {
            long availableSeats = seatService.getAvailableSeatCount(flightId);
            long bookedSeats = seatService.getBookedSeatCount(flightId);
            
            SeatStatistics stats = new SeatStatistics(availableSeats, bookedSeats, 
                availableSeats + bookedSeats);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Inner class for seat statistics
     */
    public static class SeatStatistics {
        private long availableSeats;
        private long bookedSeats;
        private long totalSeats;
        
        public SeatStatistics(long availableSeats, long bookedSeats, long totalSeats) {
            this.availableSeats = availableSeats;
            this.bookedSeats = bookedSeats;
            this.totalSeats = totalSeats;
        }
        
        // Getters
        public long getAvailableSeats() { return availableSeats; }
        public long getBookedSeats() { return bookedSeats; }
        public long getTotalSeats() { return totalSeats; }
        
        // Setters
        public void setAvailableSeats(long availableSeats) { this.availableSeats = availableSeats; }
        public void setBookedSeats(long bookedSeats) { this.bookedSeats = bookedSeats; }
        public void setTotalSeats(long totalSeats) { this.totalSeats = totalSeats; }
    }
}