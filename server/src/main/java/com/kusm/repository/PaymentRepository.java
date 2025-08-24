package com.kusm.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kusm.model.Payment;
import com.kusm.model.Payment.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by transaction ID
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Find all payments for a reservation ordered by payment date (newest
     * first)
     */
    List<Payment> findByReservationIdOrderByPaymentDateDesc(Long reservationId);

    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Find payments by payment method
     */
    List<Payment> findByPaymentMethod(String paymentMethod);

    /**
     * Find successful payments for a reservation
     */
    @Query("SELECT p FROM Payment p WHERE p.reservationId = :reservationId AND p.status = 'SUCCESS'")
    List<Payment> findSuccessfulPaymentsByReservationId(@Param("reservationId") Long reservationId);

    /**
     * Find refunds for a specific transaction
     */
    List<Payment> findByOriginalTransactionId(String originalTransactionId);

    /**
     * Find payments within a date range
     */
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate DESC")
    List<Payment> findPaymentsByDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find payments by card last four digits
     */
    List<Payment> findByCardLastFour(String cardLastFour);

    /**
     * Find payments by card holder name
     */
    List<Payment> findByCardHolderNameContainingIgnoreCase(String cardHolderName);

    /**
     * Get total amount paid for a reservation (successful payments only)
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.reservationId = :reservationId AND p.status = 'SUCCESS'")
    BigDecimal getTotalPaidAmountForReservation(@Param("reservationId") Long reservationId);

    /**
     * Get total refunded amount for a reservation
     */
    @Query("SELECT COALESCE(SUM(ABS(p.amount)), 0) FROM Payment p WHERE p.reservationId = :reservationId AND p.status = 'SUCCESS' AND p.amount < 0")
    BigDecimal getTotalRefundedAmountForReservation(@Param("reservationId") Long reservationId);

    /**
     * Find failed payments that can be retried
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.paymentDate > :cutoffDate")
    List<Payment> findRetriableFailedPayments(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find pending payments older than specified time
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.paymentDate < :cutoffDate")
    List<Payment> findStalePendingPayments(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get payment statistics for a date range
     */
    @Query("SELECT "
            + "COUNT(p) as totalTransactions, "
            + "SUM(CASE WHEN p.status = 'SUCCESS' THEN 1 ELSE 0 END) as successfulTransactions, "
            + "SUM(CASE WHEN p.status = 'FAILED' THEN 1 ELSE 0 END) as failedTransactions, "
            + "SUM(CASE WHEN p.status = 'PENDING' THEN 1 ELSE 0 END) as pendingTransactions, "
            + "SUM(CASE WHEN p.status = 'SUCCESS' THEN p.amount ELSE 0 END) as totalAmount "
            + "FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    Object[] getPaymentStatistics(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find payments by amount range
     */
    @Query("SELECT p FROM Payment p WHERE p.amount BETWEEN :minAmount AND :maxAmount ORDER BY p.amount DESC")
    List<Payment> findPaymentsByAmountRange(@Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Count payments by status for a specific date
     */
    @Query("SELECT p.status, COUNT(p) FROM Payment p WHERE DATE(p.paymentDate) = DATE(:date) GROUP BY p.status")
    List<Object[]> countPaymentsByStatusForDate(@Param("date") LocalDateTime date);

    /**
     * Find payments that need gateway synchronization (pending for too long)
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.paymentDate < :cutoffTime AND p.paymentGatewayResponse IS NULL")
    List<Payment> findPaymentsNeedingSync(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find duplicate transaction attempts (same reservation, amount, and recent
     * time)
     */
    @Query("SELECT p FROM Payment p WHERE p.reservationId = :reservationId AND p.amount = :amount "
            + "AND p.paymentDate > :recentTime AND p.status IN ('SUCCESS', 'PENDING')")
    List<Payment> findDuplicateTransactionAttempts(@Param("reservationId") Long reservationId,
            @Param("amount") BigDecimal amount,
            @Param("recentTime") LocalDateTime recentTime);

    /**
     * Check if reservation has any successful payment
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.reservationId = :reservationId AND p.status = 'SUCCESS'")
    boolean hasSuccessfulPayment(@Param("reservationId") Long reservationId);

    /**
     * Get net payment amount for reservation (payments minus refunds)
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.reservationId = :reservationId AND p.status = 'SUCCESS'")
    BigDecimal getNetPaymentAmount(@Param("reservationId") Long reservationId);

    /**
     * Find recent transactions for fraud detection
     */
    @Query("SELECT p FROM Payment p WHERE p.cardLastFour = :cardLastFour "
            + "AND p.paymentDate > :recentTime ORDER BY p.paymentDate DESC")
    List<Payment> findRecentTransactionsByCard(@Param("cardLastFour") String cardLastFour,
            @Param("recentTime") LocalDateTime recentTime);
}
