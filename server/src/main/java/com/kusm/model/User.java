package com.kusm.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(unique = true)
    private String phoneNumber;
    
    // OTP related fields
    @Column(length = 6)
    private String otp;
    
    private LocalDateTime otpGeneratedAt;
    
    private LocalDateTime otpExpiresAt;
    
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer otpAttempts = 0;
    
    // Account status fields
    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isVerified = false;
    
    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean isActive = true;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private UserStatus status = UserStatus.PENDING;
    
    // Audit fields
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;
    
    // Enum for user status
    public enum UserStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DEACTIVATED
    }
    
    // Additional constructors
    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public User(String name, String email, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper methods for OTP management
    public boolean isOtpExpired() {
        return otpExpiresAt != null && LocalDateTime.now().isAfter(otpExpiresAt);
    }
    
    public boolean isOtpValid(String providedOtp) {
        return otp != null && otp.equals(providedOtp) && !isOtpExpired();
    }
    
    public void incrementOtpAttempts() {
        this.otpAttempts = (this.otpAttempts == null) ? 1 : this.otpAttempts + 1;
    }
    
    public void resetOtpAttempts() {
        this.otpAttempts = 0;
    }
    
    public boolean isMaxOtpAttemptsReached() {
        return otpAttempts != null && otpAttempts >= 3; // Max 3 attempts
    }
}