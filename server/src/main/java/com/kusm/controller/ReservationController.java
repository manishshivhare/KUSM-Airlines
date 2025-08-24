package com.kusm.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusm.model.Reservation;
import com.kusm.model.Seat;
import com.kusm.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:3000")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    @Autowired
    private ReservationService reservationService;

    @PostMapping("/flight/{flightId}/with-payment")
    public ResponseEntity<?> createReservationWithPayment(
            @RequestBody ReservationRequest request,
            @PathVariable Long flightId) {
        try {
            logger.info("Creating reservation with payment for flight: {}", flightId);
            
            // Validate payment information
            if (request.getCardNumber() == null || request.getCardNumber().trim().isEmpty() ||
                request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Error: Payment information is required");
            }
            
            // Validate passenger information
            if (request.getPassengerName() == null || request.getPassengerName().trim().isEmpty() ||
                request.getPassengerEmail() == null || request.getPassengerEmail().trim().isEmpty() ||
                request.getPassengerPhone() == null || request.getPassengerPhone().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Error: Passenger information is required");
            }
            
            // Create reservation object
            Reservation reservation = new Reservation();
            reservation.setPassengerName(request.getPassengerName());
            reservation.setPassengerEmail(request.getPassengerEmail());
            reservation.setPassengerPhone(request.getPassengerPhone());
            reservation.setPreferredSeatClass(request.getPreferredSeatClass() != null 
                ? request.getPreferredSeatClass() : Seat.SeatClass.ECONOMY);
            
            // Process reservation with payment
            Reservation savedReservation = reservationService.createReservationWithPayment(
                reservation, flightId, request.getCardNumber(), request.getCardHolderName());
                
            logger.info("Reservation created successfully: {}", savedReservation.getBookingReference());
            return ResponseEntity.ok(savedReservation);
            
        } catch (RuntimeException e) {
            logger.error("Failed to create reservation with payment: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error creating reservation: " + e.getMessage());
        }
    }

    @PostMapping("/flight/{flightId}/with-payment/seat/{seatNumber}")
    public ResponseEntity<?> createReservationWithSpecificSeatAndPayment(
            @RequestBody ReservationRequest request,
            @PathVariable Long flightId,
            @PathVariable String seatNumber) {
        try {
            logger.info("Creating reservation with specific seat {} and payment for flight: {}", seatNumber, flightId);
            
            // Validate payment information
            if (request.getCardNumber() == null || request.getCardNumber().trim().isEmpty() ||
                request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Error: Payment information is required");
            }
            
            // Validate passenger information
            if (request.getPassengerName() == null || request.getPassengerName().trim().isEmpty() ||
                request.getPassengerEmail() == null || request.getPassengerEmail().trim().isEmpty() ||
                request.getPassengerPhone() == null || request.getPassengerPhone().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("Error: Passenger information is required");
            }
            
            // Create reservation object
            Reservation reservation = new Reservation();
            reservation.setPassengerName(request.getPassengerName());
            reservation.setPassengerEmail(request.getPassengerEmail());
            reservation.setPassengerPhone(request.getPassengerPhone());
            reservation.setPreferredSeatClass(request.getPreferredSeatClass() != null 
                ? request.getPreferredSeatClass() : Seat.SeatClass.ECONOMY);
            
            // Process reservation with specific seat and payment
            Reservation savedReservation = reservationService.createReservationWithSpecificSeat(
                reservation, flightId, seatNumber, request.getCardNumber(), request.getCardHolderName());
                
            logger.info("Reservation created successfully with seat {}: {}", seatNumber, savedReservation.getBookingReference());
            return ResponseEntity.ok(savedReservation);
            
        } catch (RuntimeException e) {
            logger.error("Failed to create reservation with specific seat and payment: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error creating reservation: " + e.getMessage());
        }
    }

    @PostMapping("/flight/{flightId}")
    public ResponseEntity<?> createReservationWithoutPayment(
            @RequestBody Reservation reservation,
            @PathVariable Long flightId) {
        try {
            logger.info("Creating reservation WITHOUT payment for flight: {}", flightId);
            Reservation savedReservation = reservationService.createReservation(reservation, flightId);
            return ResponseEntity.ok(savedReservation);
        } catch (RuntimeException e) {
            logger.error("Failed to create reservation without payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error creating reservation: " + e.getMessage());
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getReservationsByEmail(@PathVariable String email) {
        try {
            List<Reservation> reservations = reservationService.getReservationsByEmail(email);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            logger.error("Error fetching reservations for email {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error fetching reservations: " + e.getMessage());
        }
    }

    @GetMapping("/reference/{bookingReference}")
    public ResponseEntity<?> getReservationByReference(@PathVariable String bookingReference) {
        try {
            return reservationService.getReservationByReference(bookingReference)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching reservation {}: {}", bookingReference, e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error fetching reservation: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllReservations() {
        try {
            List<Reservation> reservations = reservationService.getAllReservations();
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            logger.error("Error fetching all reservations: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error fetching reservations: " + e.getMessage());
        }
    }

    @DeleteMapping("/cancel/{bookingReference}")
    public ResponseEntity<?> cancelReservation(@PathVariable String bookingReference) {
        try {
            boolean cancelled = reservationService.cancelReservation(bookingReference);
            if (cancelled) {
                logger.info("Reservation {} cancelled successfully", bookingReference);
                return ResponseEntity.ok().build();
            } else {
                logger.warn("Reservation {} not found for cancellation", bookingReference);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error cancelling reservation {}: {}", bookingReference, e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error cancelling reservation: " + e.getMessage());
        }
    }

    @PostMapping("/change-seat/{bookingReference}/{newSeatNumber}")
    public ResponseEntity<?> changeSeat(@PathVariable String bookingReference, 
            @PathVariable String newSeatNumber) {
        try {
            boolean changed = reservationService.changeSeat(bookingReference, newSeatNumber);
            if (changed) {
                logger.info("Seat changed successfully for reservation {} to seat {}", 
                    bookingReference, newSeatNumber);
                return ResponseEntity.ok("Seat changed successfully");
            } else {
                logger.warn("Failed to change seat for reservation {}", bookingReference);
                return ResponseEntity.badRequest().body("Failed to change seat");
            }
        } catch (Exception e) {
            logger.error("Error changing seat for reservation {}: {}", bookingReference, e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error changing seat: " + e.getMessage());
        }
    }

    @GetMapping("/flight/{flightId}/available-seats")
    public ResponseEntity<?> getAvailableSeats(@PathVariable Long flightId) {
        try {
            List<Seat> availableSeats = reservationService.getAvailableSeatsForFlight(flightId);
            return ResponseEntity.ok(availableSeats);
        } catch (Exception e) {
            logger.error("Error fetching available seats for flight {}: {}", flightId, e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error fetching available seats: " + e.getMessage());
        }
    }

    @GetMapping("/flight/{flightId}/seat-map")
    public ResponseEntity<?> getSeatMap(@PathVariable Long flightId) {
        try {
            List<List<Seat>> seatMap = reservationService.getSeatMapForFlight(flightId);
            return ResponseEntity.ok(seatMap);
        } catch (Exception e) {
            logger.error("Error fetching seat map for flight {}: {}", flightId, e.getMessage());
            return ResponseEntity.badRequest()
                .body("Error fetching seat map: " + e.getMessage());
        }
    }

    // Request DTO for reservation with payment
    public static class ReservationRequest {
        private String passengerName;
        private String passengerEmail;
        private String passengerPhone;
        private Seat.SeatClass preferredSeatClass;
        private String cardNumber;
        private String cardHolderName;

        // Constructors
        public ReservationRequest() {}

        public ReservationRequest(String passengerName, String passengerEmail, String passengerPhone,
                Seat.SeatClass preferredSeatClass, String cardNumber, String cardHolderName) {
            this.passengerName = passengerName;
            this.passengerEmail = passengerEmail;
            this.passengerPhone = passengerPhone;
            this.preferredSeatClass = preferredSeatClass;
            this.cardNumber = cardNumber;
            this.cardHolderName = cardHolderName;
        }

        // Getters and Setters
        public String getPassengerName() { return passengerName; }
        public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

        public String getPassengerEmail() { return passengerEmail; }
        public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

        public String getPassengerPhone() { return passengerPhone; }
        public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }

        public Seat.SeatClass getPreferredSeatClass() { return preferredSeatClass; }
        public void setPreferredSeatClass(Seat.SeatClass preferredSeatClass) { this.preferredSeatClass = preferredSeatClass; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getCardHolderName() { return cardHolderName; }
        public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

        @Override
        public String toString() {
            return "ReservationRequest{" +
                    "passengerName='" + passengerName + '\'' +
                    ", passengerEmail='" + passengerEmail + '\'' +
                    ", passengerPhone='" + passengerPhone + '\'' +
                    ", preferredSeatClass=" + preferredSeatClass +
                    ", cardHolderName='" + cardHolderName + '\'' +
                    ", cardNumber='****" + (cardNumber != null && cardNumber.length() > 4 ? 
                        cardNumber.substring(cardNumber.length() - 4) : "****") + '\'' +
                    '}';
        }
    }
}