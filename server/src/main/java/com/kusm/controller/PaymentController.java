package com.kusm.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusm.model.Payment;
import com.kusm.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * Process payment with minimal required fields
     */
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest request) {
        logger.info("Processing payment for reservation ID: {}", request.getReservationId());

        try {
            // Simple validation
            if (request.getReservationId() == null || 
                request.getCardNumber() == null || request.getCardNumber().trim().isEmpty() ||
                request.getCardHolderName() == null || request.getCardHolderName().trim().isEmpty() ||
                request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                
                return ResponseEntity.badRequest()
                    .body(new PaymentResponse(false, "Missing required payment information"));
            }

            Payment payment = paymentService.processPayment(
                request.getReservationId(),
                request.getCardNumber(),
                request.getCardHolderName(),
                request.getAmount()
            );

            logger.info("Payment successful for reservation {} with transaction ID: {}", 
                request.getReservationId(), payment.getTransactionId());

            return ResponseEntity.ok(new PaymentResponse(payment));

        } catch (Exception e) {
            logger.error("Payment failed for reservation {}: {}", 
                request.getReservationId(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(new PaymentResponse(false, e.getMessage()));
        }
    }

    /**
     * Get payment by transaction ID
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getPayment(@PathVariable String transactionId) {
        try {
            Optional<Payment> payment = paymentService.getPaymentByTransactionId(transactionId);
            
            if (payment.isPresent()) {
                return ResponseEntity.ok(new PaymentResponse(payment.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching payment {}: {}", transactionId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new PaymentResponse(false, "Error retrieving payment"));
        }
    }

    /**
     * Get payment history for a reservation
     */
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<?> getPaymentHistory(@PathVariable Long reservationId) {
        try {
            List<Payment> payments = paymentService.getPaymentsByReservationId(reservationId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error fetching payment history for reservation {}: {}", 
                reservationId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new PaymentResponse(false, "Error retrieving payment history"));
        }
    }

    // Simple Request DTO
    public static class PaymentRequest {
        private Long reservationId;
        private String cardNumber;
        private String cardHolderName;
        private BigDecimal amount;

        // Constructors
        public PaymentRequest() {}

        public PaymentRequest(Long reservationId, String cardNumber, 
                String cardHolderName, BigDecimal amount) {
            this.reservationId = reservationId;
            this.cardNumber = cardNumber;
            this.cardHolderName = cardHolderName;
            this.amount = amount;
        }

        // Getters and Setters
        public Long getReservationId() { return reservationId; }
        public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

        public String getCardNumber() { return cardNumber; }
        public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

        public String getCardHolderName() { return cardHolderName; }
        public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    // Simple Response DTO
    public static class PaymentResponse {
        private boolean success;
        private String message;
        private String transactionId;
        private BigDecimal amount;
        private String cardLastFour;

        // Constructors
        public PaymentResponse() {}

        public PaymentResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public PaymentResponse(Payment payment) {
            this.success = payment.isSuccessful();
            this.message = payment.isSuccessful() ? "Payment processed successfully" : "Payment failed";
            this.transactionId = payment.getTransactionId();
            this.amount = payment.getAmount();
            this.cardLastFour = payment.getCardLastFour();
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getCardLastFour() { return cardLastFour; }
        public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }
    }
}