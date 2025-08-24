package com.kusm.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusm.model.Payment;
import com.kusm.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Process payment with basic card validation
     */
    @Transactional
    public Payment processPayment(Long reservationId, String cardNumber, 
            String cardHolderName, BigDecimal amount) {
        
        logger.info("Processing payment for reservation ID: {} with amount: {}", reservationId, amount);

        // Basic validation
        if (!isValidCardNumber(cardNumber)) {
            throw new RuntimeException("Invalid card number");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid payment amount");
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setReservationId(reservationId);
        payment.setTransactionId(generateTransactionId());
        payment.setPaymentMethod("CREDIT_CARD");
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setCardLastFour(getLastFourDigits(cardNumber));
        payment.setCardHolderName(cardHolderName);

        // Simple payment processing - always success for valid inputs
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setPaymentGatewayResponse("APPROVED");

        logger.info("Payment successful for reservation {} with transaction ID: {}", 
                reservationId, payment.getTransactionId());

        return paymentRepository.save(payment);
    }

    /**
     * Get payment by transaction ID
     */
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    /**
     * Get all payments for a reservation
     */
    public List<Payment> getPaymentsByReservationId(Long reservationId) {
        return paymentRepository.findByReservationIdOrderByPaymentDateDesc(reservationId);
    }

    /**
     * Basic card number validation using Luhn algorithm
     */
    public boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) {
            return false;
        }

        // Remove spaces and dashes
        String cleanCard = cardNumber.replaceAll("[\\s-]", "");

        // Check if it contains only digits and has valid length (13-19 digits)
        if (!cleanCard.matches("\\d+") || cleanCard.length() < 13 || cleanCard.length() > 19) {
            return false;
        }

        // Luhn algorithm check
        return luhnCheck(cleanCard);
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Get last four digits of card number
     */
    private String getLastFourDigits(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String cleaned = cardNumber.replaceAll("[\\s-]", "");
        return cleaned.substring(cleaned.length() - 4);
    }

    /**
     * Luhn algorithm for card validation
     * FIXED: Corrected the algorithm - when digit > 9, it should be (digit / 10) + (digit % 10)
     */
    private boolean luhnCheck(String cardNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10); // FIXED: Correct Luhn calculation
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum % 10) == 0;
    }
}