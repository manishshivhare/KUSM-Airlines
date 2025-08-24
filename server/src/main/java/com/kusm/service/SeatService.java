package com.kusm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusm.model.Flight;
import com.kusm.model.Reservation;
import com.kusm.model.Seat;
import com.kusm.model.Seat.SeatClass;
import com.kusm.model.Seat.SeatStatus;
import com.kusm.repository.SeatRepository;

@Service
public class SeatService {
    
    @Autowired
    private SeatRepository seatRepository;
    
    /**
     * Initialize seats for a flight based on aircraft configuration
     * Default configuration: 6 seats per row (A-F), Economy class
     */
    @Transactional
    public void initializeSeatsForFlight(Flight flight) {
        if (flight == null || flight.getId() == null) {
            throw new IllegalArgumentException("Flight cannot be null and must have an ID");
        }
        
        List<Seat> existingSeats = seatRepository.findByFlightId(flight.getId());
        if (existingSeats.isEmpty()) {
            List<Seat> seats = generateSeatsForFlight(flight);
            System.out.println("Initializing " + seats.size() + " seats for flight: " + flight.getId());
            seatRepository.saveAll(seats);
        } else {
            System.out.println("Seats already initialized for flight: " + flight.getId() + " (found " + existingSeats.size() + " seats)");
        }
    }
    
    /**
     * Generate seats for a flight based on total seats
     */
    private List<Seat> generateSeatsForFlight(Flight flight) {
        List<Seat> seats = new ArrayList<>();
        int totalSeats = flight.getTotalSeats();
        int seatsPerRow = 6; 
        String[] seatLetters = {"A", "B", "C", "D", "E", "F"};
        
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("Flight must have at least 1 seat");
        }
        
        int rows = (int) Math.ceil((double) totalSeats / seatsPerRow);
        int seatCount = 0;
        
        for (int row = 1; row <= rows && seatCount < totalSeats; row++) {
            for (int seat = 0; seat < seatsPerRow && seatCount < totalSeats; seat++) {
                String seatNumber = row + seatLetters[seat];
                SeatClass seatClass = determineSeatClass(row, totalSeats);
                
                Seat newSeat = new Seat(seatNumber, seatClass, flight);
                seats.add(newSeat);
                seatCount++;
            }
        }
        
        System.out.println("Generated " + seats.size() + " seats for flight " + flight.getFlightNumber());
        return seats;
    }
    
    /**
     * Determine seat class based on row number
     */
    private SeatClass determineSeatClass(int row, int totalSeats) {
        int totalRows = (int) Math.ceil((double) totalSeats / 6);
        
        // First class: first 2 rows
        if (row <= 2) {
            return SeatClass.FIRST;
        } 
        // Business class: up to 15% of total rows (minimum 2 more rows)
        else if (row <= Math.max(4, (int) (totalRows * 0.15))) {
            return SeatClass.BUSINESS;
        } 
        // Premium economy: up to 30% of total rows (minimum 4 more rows)
        else if (row <= Math.max(8, (int) (totalRows * 0.30))) {
            return SeatClass.PREMIUM_ECONOMY;
        } 
        // Economy: remaining rows
        else {
            return SeatClass.ECONOMY;
        }
    }
    
    /**
     * Get available seats for a flight
     */
    public List<Seat> getAvailableSeats(Long flightId) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        return seatRepository.findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE);
    }
    
    /**
     * Get available seats by class
     */
    public List<Seat> getAvailableSeatsByClass(Long flightId, SeatClass seatClass) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        if (seatClass == null) {
            return getAvailableSeats(flightId);
        }
        return seatRepository.findByFlightIdAndSeatClassAndStatus(flightId, seatClass, SeatStatus.AVAILABLE);
    }
    
    /**
     * Get all seats for a flight
     */
    public List<Seat> getAllSeatsForFlight(Long flightId) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        return seatRepository.findByFlightId(flightId);
    }
    
    /**
     * Assign a seat to a reservation
     */
    @Transactional
    public Seat assignSeat(Long flightId, String seatNumber, Reservation reservation) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        if (seatNumber == null || seatNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Seat number cannot be null or empty");
        }
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }
        
        Optional<Seat> seatOpt = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber.trim().toUpperCase());
        
        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            if (seat.isAvailable()) {
                seat.setStatus(SeatStatus.BOOKED);
                seat.setReservation(reservation);
                System.out.println("Assigned seat " + seatNumber + " to reservation " + reservation.getBookingReference());
                return seatRepository.save(seat);
            } else {
                throw new IllegalStateException("Seat " + seatNumber + " is not available (current status: " + seat.getStatus() + ")");
            }
        } else {
            throw new IllegalArgumentException("Seat " + seatNumber + " does not exist on flight " + flightId);
        }
    }
    
    /**
     * Auto-assign next available seat (preferably window or aisle)
     */
    @Transactional
    public Seat autoAssignSeat(Long flightId, Reservation reservation, SeatClass preferredClass) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }
        
        List<Seat> availableSeats;
        
        if (preferredClass != null) {
            availableSeats = seatRepository.findAvailableSeatsByFlightIdAndSeatClassOrderBySeatNumber(flightId, preferredClass);
            System.out.println("Looking for available " + preferredClass + " seats on flight " + flightId);
        } else {
            availableSeats = seatRepository.findAvailableSeatsByFlightIdOrderBySeatNumber(flightId);
            System.out.println("Looking for any available seats on flight " + flightId);
        }
        
        if (availableSeats.isEmpty()) {
            if (preferredClass != null) {
                // Try to find seats in any class if preferred class is not available
                System.out.println("No " + preferredClass + " seats available, trying any class");
                availableSeats = seatRepository.findAvailableSeatsByFlightIdOrderBySeatNumber(flightId);
                if (availableSeats.isEmpty()) {
                    throw new IllegalStateException("No available seats on flight " + flightId);
                }
            } else {
                throw new IllegalStateException("No available seats on flight " + flightId);
            }
        }
        
        // Prefer window seats, then aisle, then middle
        Seat selectedSeat = availableSeats.stream()
            .filter(Seat::isWindow)
            .findFirst()
            .orElse(availableSeats.stream()
                .filter(Seat::isAisle)
                .findFirst()
                .orElse(availableSeats.get(0)));
        
        selectedSeat.setStatus(SeatStatus.BOOKED);
        selectedSeat.setReservation(reservation);
        
        System.out.println("Auto-assigned seat " + selectedSeat.getSeatNumber() + 
                         " (" + selectedSeat.getSeatClass() + ") to reservation " + reservation.getBookingReference());
        
        return seatRepository.save(selectedSeat);
    }
    
    /**
     * Release seat when reservation is cancelled
     */
    @Transactional
    public void releaseSeat(Long seatId) {
        if (seatId == null) {
            throw new IllegalArgumentException("Seat ID cannot be null");
        }
        
        Optional<Seat> seatOpt = seatRepository.findById(seatId);
        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            String seatNumber = seat.getSeatNumber();
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setReservation(null);
            seatRepository.save(seat);
            System.out.println("Released seat " + seatNumber);
        } else {
            System.out.println("Seat with ID " + seatId + " not found for release");
        }
    }
    
    /**
     * Release seats for a reservation
     */
    @Transactional
    public void releaseSeatsForReservation(Long reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        
        List<Seat> seats = seatRepository.findByReservationId(reservationId);
        if (!seats.isEmpty()) {
            for (Seat seat : seats) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setReservation(null);
            }
            seatRepository.saveAll(seats);
            System.out.println("Released " + seats.size() + " seats for reservation " + reservationId);
        } else {
            System.out.println("No seats found for reservation " + reservationId);
        }
    }
    
    /**
     * Get seat map for a flight
     */
    public List<List<Seat>> getSeatMap(Long flightId) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        
        List<Seat> allSeats = seatRepository.findByFlightId(flightId);
        if (allSeats.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<List<Seat>> seatMap = new ArrayList<>();
        
        // Group seats by row
        int currentRow = -1;
        List<Seat> currentRowSeats = new ArrayList<>();
        
        for (Seat seat : allSeats) {
            try {
                int seatRow = Integer.parseInt(seat.getSeatNumber().replaceAll("[A-Z]", ""));
                
                if (seatRow != currentRow) {
                    // Start a new row
                    if (!currentRowSeats.isEmpty()) {
                        seatMap.add(new ArrayList<>(currentRowSeats));
                    }
                    currentRowSeats.clear();
                    currentRow = seatRow;
                }
                
                currentRowSeats.add(seat);
            } catch (NumberFormatException e) {
                System.err.println("Invalid seat number format: " + seat.getSeatNumber());
                // Skip invalid seat numbers
            }
        }
        
        // Add the last row
        if (!currentRowSeats.isEmpty()) {
            seatMap.add(currentRowSeats);
        }
        
        return seatMap;
    }
    
    /**
     * Get seat count by status
     */
    public long getAvailableSeatCount(Long flightId) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        return seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE);
    }
    
    public long getBookedSeatCount(Long flightId) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        return seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.BOOKED);
    }
    
    public long getBlockedSeatCount(Long flightId) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        return seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.BLOCKED);
    }
    
    /**
     * Get seat statistics for a flight
     */
    public SeatStatistics getSeatStatistics(Long flightId) {
        if (flightId == null) {
            throw new IllegalArgumentException("Flight ID cannot be null");
        }
        
        long available = getAvailableSeatCount(flightId);
        long booked = getBookedSeatCount(flightId);
        long blocked = getBlockedSeatCount(flightId);
        long total = available + booked + blocked;
        
        return new SeatStatistics(total, available, booked, blocked);
    }
    
    /**
     * Block/unblock a seat (useful for maintenance or VIP reservations)
     */
    @Transactional
    public void blockSeat(Long flightId, String seatNumber) {
        if (flightId == null || seatNumber == null) {
            throw new IllegalArgumentException("Flight ID and seat number cannot be null");
        }
        
        Optional<Seat> seatOpt = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber.trim().toUpperCase());
        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                seat.setStatus(SeatStatus.BLOCKED);
                seatRepository.save(seat);
                System.out.println("Blocked seat " + seatNumber);
            } else {
                throw new IllegalStateException("Cannot block seat " + seatNumber + " - current status: " + seat.getStatus());
            }
        } else {
            throw new IllegalArgumentException("Seat " + seatNumber + " not found");
        }
    }
    
    @Transactional
    public void unblockSeat(Long flightId, String seatNumber) {
        if (flightId == null || seatNumber == null) {
            throw new IllegalArgumentException("Flight ID and seat number cannot be null");
        }
        
        Optional<Seat> seatOpt = seatRepository.findByFlightIdAndSeatNumber(flightId, seatNumber.trim().toUpperCase());
        if (seatOpt.isPresent()) {
            Seat seat = seatOpt.get();
            if (seat.getStatus() == SeatStatus.BLOCKED) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);
                System.out.println("Unblocked seat " + seatNumber);
            } else {
                throw new IllegalStateException("Cannot unblock seat " + seatNumber + " - current status: " + seat.getStatus());
            }
        } else {
            throw new IllegalArgumentException("Seat " + seatNumber + " not found");
        }
    }
    
    /**
     * Get seats by reservation ID
     */
    public List<Seat> getSeatsByReservationId(Long reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        return seatRepository.findByReservationId(reservationId);
    }
    
    /**
     * Inner class for seat statistics
     */
    public static class SeatStatistics {
        private final long total;
        private final long available;
        private final long booked;
        private final long blocked;
        
        public SeatStatistics(long total, long available, long booked, long blocked) {
            this.total = total;
            this.available = available;
            this.booked = booked;
            this.blocked = blocked;
        }
        
        public long getTotal() { return total; }
        public long getAvailable() { return available; }
        public long getBooked() { return booked; }
        public long getBlocked() { return blocked; }
        
        public double getOccupancyRate() {
            return total > 0 ? (double) booked / total * 100 : 0;
        }
        
        @Override
        public String toString() {
            return String.format("SeatStatistics{total=%d, available=%d, booked=%d, blocked=%d, occupancy=%.1f%%}", 
                               total, available, booked, blocked, getOccupancyRate());
        }
    }
}