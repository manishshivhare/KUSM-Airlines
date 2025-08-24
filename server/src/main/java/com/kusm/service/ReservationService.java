package com.kusm.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusm.model.Flight;
import com.kusm.model.Payment;
import com.kusm.model.Reservation;
import com.kusm.model.Seat;
import com.kusm.repository.ReservationRepository;

@Service
public class ReservationService {

    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private FlightService flightService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private PaymentService paymentService;

    @Transactional
    public Reservation createReservationWithPayment(Reservation reservation, Long flightId, 
            String cardNumber, String cardHolderName) {
        
        logger.info("Creating reservation with payment for flight ID: {}", flightId);

        Optional<Flight> flightOpt = flightService.getFlightById(flightId);
        if (flightOpt.isPresent()) {
            Flight flight = flightOpt.get();
            logger.info("Found flight: {} with {} total seats", flight.getFlightNumber(), flight.getTotalSeats());

            // Initialize seats for the flight if not already done
            seatService.initializeSeatsForFlight(flight);

            // Check if seats are available
            long availableSeats = seatService.getAvailableSeatCount(flightId);
            logger.info("Available seats count: {}", availableSeats);

            if (availableSeats <= 0) {
                logger.warn("No available seats for flight {}", flightId);
                throw new RuntimeException("No available seats on this flight");
            }

            // Set reservation details but keep status as PENDING until payment is confirmed
            reservation.setFlight(flight);
            reservation.setBookingReference(generateBookingReference());
            reservation.setTotalAmount(flight.getPrice());
            reservation.setStatus(Reservation.BookingStatus.PENDING);

            // Save reservation first to get the ID
            logger.info("Saving reservation with booking reference: {}", reservation.getBookingReference());
            Reservation savedReservation = reservationRepository.save(reservation);
            logger.info("Reservation saved with ID: {}", savedReservation.getId());

            try {
                // Process payment
                logger.info("Processing payment for reservation ID: {} with amount: {}", 
                    savedReservation.getId(), flight.getPrice());
                
                Payment payment = paymentService.processPayment(
                    savedReservation.getId(), 
                    cardNumber, 
                    cardHolderName, 
                    flight.getPrice()
                );

                // FIXED: Added null check for payment
                if (payment != null && payment.isSuccessful()) {
                    // Payment successful - confirm reservation and assign seat
                    logger.info("Payment successful with transaction ID: {}", payment.getTransactionId());
                    savedReservation.setStatus(Reservation.BookingStatus.CONFIRMED);
                    
                    // Auto-assign a seat
                    logger.info("Attempting to auto-assign seat for reservation ID: {} with preferred class: {}",
                            savedReservation.getId(), reservation.getPreferredSeatClass());

                    Seat assignedSeat = seatService.autoAssignSeat(flightId, savedReservation,
                            reservation.getPreferredSeatClass());

                    logger.info("Successfully assigned seat: {} (ID: {}) to reservation: {}",
                            assignedSeat.getSeatNumber(), assignedSeat.getId(), savedReservation.getBookingReference());

                    // Update flight's available seats count
                    flightService.updateAvailableSeats(flightId, 1);

                    // Save updated reservation
                    savedReservation = reservationRepository.save(savedReservation);
                    
                    // Force a flush to ensure changes are persisted
                    reservationRepository.flush();
                    
                    logger.info("Reservation {} confirmed with payment transaction: {}", 
                        savedReservation.getBookingReference(), payment.getTransactionId());
                    
                    // Refresh the reservation to get the updated seats
                    Reservation finalReservation = getReservationWithSeats(savedReservation.getId());
                    
                    if (finalReservation.getSeats().isEmpty()) {
                        logger.error("ERROR: Seats list is empty after assignment for reservation {}",
                                finalReservation.getBookingReference());

                        // Try alternative approach - manually fetch seats
                        List<Seat> seats = seatService.getSeatsByReservationId(finalReservation.getId());
                        logger.info("Manually fetched {} seats for reservation {}", seats.size(), finalReservation.getId());

                        if (!seats.isEmpty()) {
                            finalReservation.setSeats(seats);
                            logger.info("Manually set seats. Final seat numbers: {}", finalReservation.getSeatNumbers());
                        }
                    }
                    
                    return finalReservation;
                    
                } else {
                    // Payment failed - cancel reservation
                    String failureReason = (payment != null) ? payment.getFailureReason() : "Payment returned null";
                    logger.warn("Payment failed for reservation {}: {}", 
                        savedReservation.getBookingReference(), failureReason);
                    
                    savedReservation.setStatus(Reservation.BookingStatus.CANCELLED);
                    reservationRepository.save(savedReservation);
                    
                    throw new RuntimeException("Payment failed. Reservation has been cancelled.");
                }
                
            } catch (Exception e) {
                // Payment processing failed - cancel reservation
                logger.error("Payment processing failed for reservation {}: {}", 
                    savedReservation.getBookingReference(), e.getMessage());
                
                savedReservation.setStatus(Reservation.BookingStatus.CANCELLED);
                reservationRepository.save(savedReservation);
                
                throw new RuntimeException("Reservation failed due to payment error: " + e.getMessage(), e);
            }
        }
        
        logger.error("Flight not found with ID: {}", flightId);
        throw new RuntimeException("Flight not available");
    }

    @Transactional
    public Reservation createReservation(Reservation reservation, Long flightId) {
        logger.info("Creating reservation WITHOUT payment for flight ID: {}", flightId);

        Optional<Flight> flightOpt = flightService.getFlightById(flightId);
        if (flightOpt.isPresent()) {
            Flight flight = flightOpt.get();
            logger.info("Found flight: {} with {} total seats", flight.getFlightNumber(), flight.getTotalSeats());

            // Initialize seats for the flight if not already done
            seatService.initializeSeatsForFlight(flight);

            // Check if seats are available
            long availableSeats = seatService.getAvailableSeatCount(flightId);
            logger.info("Available seats count: {}", availableSeats);

            if (availableSeats > 0) {
                reservation.setFlight(flight);
                reservation.setBookingReference(generateBookingReference());
                reservation.setTotalAmount(flight.getPrice());
                reservation.setStatus(Reservation.BookingStatus.CONFIRMED);

                // Save reservation first to get the ID
                logger.info("Saving reservation with booking reference: {}", reservation.getBookingReference());
                Reservation savedReservation = reservationRepository.save(reservation);
                logger.info("Reservation saved with ID: {}", savedReservation.getId());

                try {
                    // Auto-assign a seat
                    logger.info("Attempting to auto-assign seat for reservation ID: {} with preferred class: {}",
                            savedReservation.getId(), reservation.getPreferredSeatClass());

                    Seat assignedSeat = seatService.autoAssignSeat(flightId, savedReservation,
                            reservation.getPreferredSeatClass());

                    logger.info("Successfully assigned seat: {} (ID: {}) to reservation: {}",
                            assignedSeat.getSeatNumber(), assignedSeat.getId(), savedReservation.getBookingReference());

                    // Update flight's available seats count
                    flightService.updateAvailableSeats(flightId, 1);

                    // Force a flush to ensure changes are persisted
                    reservationRepository.flush();

                    // Refresh the reservation to get the updated seats
                    Reservation finalReservation = getReservationWithSeats(savedReservation.getId());
                    logger.info("Final reservation has {} seats assigned", finalReservation.getSeats().size());

                    if (finalReservation.getSeats().isEmpty()) {
                        logger.error("ERROR: Seats list is empty after assignment for reservation {}",
                                finalReservation.getBookingReference());

                        // Try alternative approach - manually fetch seats
                        List<Seat> seats = seatService.getSeatsByReservationId(finalReservation.getId());
                        logger.info("Manually fetched {} seats for reservation {}", seats.size(), finalReservation.getId());

                        if (!seats.isEmpty()) {
                            finalReservation.setSeats(seats);
                            logger.info("Manually set seats. Final seat numbers: {}", finalReservation.getSeatNumbers());
                        }
                    }

                    return finalReservation;

                } catch (Exception e) {
                    logger.error("Error during seat assignment: ", e);
                    throw new RuntimeException("Seat assignment failed: " + e.getMessage(), e);
                }
            } else {
                logger.warn("No available seats for flight {}", flightId);
                throw new RuntimeException("No available seats on this flight");
            }
        }
        logger.error("Flight not found with ID: {}", flightId);
        throw new RuntimeException("Flight not available or no seats left");
    }

    @Transactional
    public Reservation createReservationWithSpecificSeat(Reservation reservation, Long flightId, 
            String seatNumber, String cardNumber, String cardHolderName) {
        
        logger.info("Creating reservation with specific seat {} and payment for flight ID: {}", seatNumber, flightId);

        Optional<Flight> flightOpt = flightService.getFlightById(flightId);
        if (flightOpt.isPresent()) {
            Flight flight = flightOpt.get();

            // Initialize seats for the flight if not already done
            seatService.initializeSeatsForFlight(flight);

            reservation.setFlight(flight);
            reservation.setBookingReference(generateBookingReference());
            reservation.setTotalAmount(flight.getPrice());
            reservation.setStatus(Reservation.BookingStatus.PENDING);

            // Save reservation first to get the ID
            Reservation savedReservation = reservationRepository.save(reservation);
            logger.info("Reservation saved with ID: {}", savedReservation.getId());

            try {
                // Process payment first
                Payment payment = paymentService.processPayment(
                    savedReservation.getId(), 
                    cardNumber, 
                    cardHolderName, 
                    flight.getPrice()
                );

                // FIXED: Added null check for payment
                if (payment != null && payment.isSuccessful()) {
                    // Payment successful - confirm reservation and assign specific seat
                    savedReservation.setStatus(Reservation.BookingStatus.CONFIRMED);
                    
                    // Assign specific seat
                    Seat assignedSeat = seatService.assignSeat(flightId, seatNumber, savedReservation);
                    logger.info("Assigned specific seat: {} to reservation: {}", seatNumber, savedReservation.getBookingReference());

                    // Update flight's available seats count
                    flightService.updateAvailableSeats(flightId, 1);

                    // Save updated reservation
                    savedReservation = reservationRepository.save(savedReservation);

                    // Refresh the reservation to get the updated seats
                    Reservation finalReservation = getReservationWithSeats(savedReservation.getId());

                    if (finalReservation.getSeats().isEmpty()) {
                        // Fallback - manually fetch seats
                        List<Seat> seats = seatService.getSeatsByReservationId(finalReservation.getId());
                        finalReservation.setSeats(seats);
                    }

                    return finalReservation;
                } else {
                    // Payment failed - cancel reservation
                    savedReservation.setStatus(Reservation.BookingStatus.CANCELLED);
                    reservationRepository.save(savedReservation);
                    throw new RuntimeException("Payment failed. Cannot assign seat " + seatNumber);
                }
            } catch (RuntimeException e) {
                // Payment or seat assignment failed - cancel reservation
                savedReservation.setStatus(Reservation.BookingStatus.CANCELLED);
                reservationRepository.save(savedReservation);
                logger.error("Cannot assign seat {}: {}", seatNumber, e.getMessage());
                throw new RuntimeException("Cannot assign seat " + seatNumber + ": " + e.getMessage());
            }
        }
        throw new RuntimeException("Flight not available");
    }

    /**
     * Get reservation with seats eagerly loaded
     */
    private Reservation getReservationWithSeats(Long reservationId) {
        logger.debug("Fetching reservation {} with seats", reservationId);

        // Try custom repository method first
        try {
            Optional<Reservation> reservationOpt = reservationRepository.findByIdWithSeats(reservationId);
            if (reservationOpt.isPresent()) {
                Reservation reservation = reservationOpt.get();
                logger.debug("Loaded reservation via findByIdWithSeats with {} seats", reservation.getSeats().size());
                return reservation;
            }
        } catch (Exception e) {
            logger.warn("Custom repository method failed, using fallback: {}", e.getMessage());
        }

        // Fallback to regular findById and manually load seats
        Optional<Reservation> reservationOpt = reservationRepository.findById(reservationId);
        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();
            // Manually load seats
            List<Seat> seats = seatService.getSeatsByReservationId(reservationId);
            reservation.setSeats(seats);
            logger.debug("Loaded reservation via fallback with {} seats", seats.size());
            return reservation;
        }

        throw new RuntimeException("Reservation not found after creation: " + reservationId);
    }

    public List<Reservation> getReservationsByEmail(String email) {
        try {
            return reservationRepository.findByPassengerEmailWithSeats(email);
        } catch (Exception e) {
            logger.warn("Custom email query failed, using fallback: {}", e.getMessage());
            List<Reservation> reservations = reservationRepository.findByPassengerEmail(email);
            for (Reservation reservation : reservations) {
                List<Seat> seats = seatService.getSeatsByReservationId(reservation.getId());
                reservation.setSeats(seats);
            }
            return reservations;
        }
    }

    public Optional<Reservation> getReservationByReference(String bookingReference) {
        try {
            Optional<Reservation> reservationOpt = reservationRepository.findByBookingReferenceWithSeats(bookingReference);
            if (reservationOpt.isPresent()) {
                return reservationOpt;
            }
        } catch (Exception e) {
            logger.warn("Custom booking reference query failed, using fallback: {}", e.getMessage());
        }

        // Fallback: load reservation and manually fetch seats
        Optional<Reservation> reservationOpt = reservationRepository.findByBookingReference(bookingReference);
        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();
            List<Seat> seats = seatService.getSeatsByReservationId(reservation.getId());
            reservation.setSeats(seats);
            return Optional.of(reservation);
        }

        return Optional.empty();
    }

    public List<Reservation> getAllReservations() {
        try {
            return reservationRepository.findAllWithSeats();
        } catch (Exception e) {
            logger.warn("Custom findAllWithSeats failed, using fallback: {}", e.getMessage());
            List<Reservation> reservations = reservationRepository.findAll();
            for (Reservation reservation : reservations) {
                List<Seat> seats = seatService.getSeatsByReservationId(reservation.getId());
                reservation.setSeats(seats);
            }
            return reservations;
        }
    }

    private String generateBookingReference() {
        return "FL" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public boolean cancelReservation(String bookingReference) {
        Optional<Reservation> reservationOpt = reservationRepository.findByBookingReference(bookingReference);
        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();
            reservation.setStatus(Reservation.BookingStatus.CANCELLED);

            // Release assigned seats
            seatService.releaseSeatsForReservation(reservation.getId());

            // Free up the seat count in flight
            Flight flight = reservation.getFlight();
            flight.setAvailableSeats(flight.getAvailableSeats() + 1);
            flightService.saveFlight(flight);

            reservationRepository.save(reservation);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean changeSeat(String bookingReference, String newSeatNumber) {
        Optional<Reservation> reservationOpt = reservationRepository.findByBookingReference(bookingReference);
        if (reservationOpt.isPresent()) {
            Reservation reservation = reservationOpt.get();

            if (reservation.getStatus() == Reservation.BookingStatus.CONFIRMED) {
                try {
                    // Release current seats
                    seatService.releaseSeatsForReservation(reservation.getId());

                    // Assign new seat
                    seatService.assignSeat(reservation.getFlight().getId(), newSeatNumber, reservation);

                    return true;
                } catch (RuntimeException e) {
                    throw new RuntimeException("Seat change failed: " + e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * Get available seats for a flight
     */
    public List<Seat> getAvailableSeatsForFlight(Long flightId) {
        return seatService.getAvailableSeats(flightId);
    }

    /**
     * Get seat map for a flight
     */
    public List<List<Seat>> getSeatMapForFlight(Long flightId) {
        return seatService.getSeatMap(flightId);
    }
}