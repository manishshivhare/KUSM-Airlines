package com.kusm.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "card_last_four")
    private String cardLastFour;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "payment_gateway_response")
    private String paymentGatewayResponse;

    // ADDED: Field to store failure reason when payment fails
    @Column(name = "failure_reason")
    private String failureReason;

    // ADDED: Field for original transaction ID (for refunds/reversals)
    @Column(name = "original_transaction_id")
    private String originalTransactionId;

    // Constructors
    public Payment() {}

    public Payment(Long reservationId, String paymentMethod, BigDecimal amount) {
        this.reservationId = reservationId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.paymentDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getPaymentGatewayResponse() {
        return paymentGatewayResponse;
    }

    public void setPaymentGatewayResponse(String paymentGatewayResponse) {
        this.paymentGatewayResponse = paymentGatewayResponse;
    }

    // ADDED: Getter and setter for failure reason
    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    // ADDED: Getter and setter for original transaction ID
    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public void setOriginalTransactionId(String originalTransactionId) {
        this.originalTransactionId = originalTransactionId;
    }

    // Utility methods
    public boolean isSuccessful() {
        return PaymentStatus.SUCCESS.equals(this.status);
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(this.status);
    }

    public boolean isPending() {
        return PaymentStatus.PENDING.equals(this.status);
    }

    // ADDED: Utility method to get failure information
    public String getFailureInfo() {
        if (isFailed()) {
            return failureReason != null ? failureReason : "Payment failed - no specific reason provided";
        }
        return null;
    }

    // Payment Status Enum
    public enum PaymentStatus {
        PENDING("Payment is being processed"),
        SUCCESS("Payment completed successfully"),
        FAILED("Payment failed");

        private final String description;

        PaymentStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", reservationId=" + reservationId +
                ", transactionId='" + transactionId + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                ", cardLastFour='" + cardLastFour + '\'' +
                ", failureReason='" + failureReason + '\'' +
                ", originalTransactionId='" + originalTransactionId + '\'' +
                '}';
    }
}