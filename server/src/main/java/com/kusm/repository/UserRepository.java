package com.kusm.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.kusm.model.User;
import com.kusm.model.User.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ----------------------- Basic Finder Methods -----------------------
    Optional<User> findByEmail(String 
    email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByStatus(UserStatus status);
    List<User> findByIsVerified(Boolean isVerified);
    List<User> findByIsActive(Boolean isActive);

    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();

    // ---------------------- OTP-Related Queries -------------------------
    Optional<User> findByEmailAndOtp(String email, String otp);
    Optional<User> findByPhoneNumberAndOtp(String phoneNumber, String otp);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.otp = :otp AND u.otpExpiresAt > :currentTime")
    Optional<User> findByEmailAndValidOtp(@Param("email") String email,
                                          @Param("otp") String otp,
                                          @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber AND u.otp = :otp AND u.otpExpiresAt > :currentTime")
    Optional<User> findByPhoneNumberAndValidOtp(@Param("phoneNumber") String phoneNumber,
                                                @Param("otp") String otp,
                                                @Param("currentTime") LocalDateTime currentTime);

    // ------------------ User Existence Checks --------------------------
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.isVerified = true")
    boolean existsByEmailAndIsVerified(@Param("email") String email);

    // ------------------ User Verification & Cleanup ---------------------
    @Query("SELECT u FROM User u WHERE u.isVerified = false AND u.createdAt < :cutoffTime")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT u FROM User u WHERE u.otpExpiresAt < :currentTime AND u.otp IS NOT NULL")
    List<User> findUsersWithExpiredOtp(@Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.isVerified = false AND u.createdAt < :cutoffTime")
    int deleteUnverifiedUsersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.otp = null, u.otpGeneratedAt = null, u.otpExpiresAt = null WHERE u.otpExpiresAt < :currentTime")
    int clearExpiredOtps(@Param("currentTime") LocalDateTime currentTime);

    // ----------------------- Update Queries ----------------------------
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.isVerified = true, u.status = 'ACTIVE', u.updatedAt = :currentTime WHERE u.id = :userId")
    int markUserAsVerified(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.otp = null, u.otpGeneratedAt = null, u.otpExpiresAt = null, u.otpAttempts = 0, u.updatedAt = :currentTime WHERE u.id = :userId")
    int clearOtpData(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.updatedAt = :currentTime WHERE u.id = :userId")
    int updateLastLoginTime(@Param("userId") Long userId,
                            @Param("loginTime") LocalDateTime loginTime,
                            @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.otpAttempts = u.otpAttempts + 1, u.updatedAt = :currentTime WHERE u.id = :userId")
    int incrementOtpAttempts(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.status = :status, u.updatedAt = :currentTime WHERE u.id = :userId")
    int updateUserStatus(@Param("userId") Long userId,
                         @Param("status") UserStatus status,
                         @Param("currentTime") LocalDateTime currentTime);

    // --------------------- Dynamic Search & Filter ---------------------
    @Query("SELECT u FROM User u WHERE " +
           "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:isVerified IS NULL OR u.isVerified = :isVerified) AND " +
           "(:isActive IS NULL OR u.isActive = :isActive)")
    List<User> findUsersWithFilters(@Param("name") String name,
                                    @Param("email") String email,
                                    @Param("status") UserStatus status,
                                    @Param("isVerified") Boolean isVerified,
                                    @Param("isActive") Boolean isActive);

    // ------------------- Count Queries for Stats -----------------------
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countUsersByDateRange(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isVerified = true")
    long countVerifiedUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countUsersByStatus(@Param("status") UserStatus status);
}
