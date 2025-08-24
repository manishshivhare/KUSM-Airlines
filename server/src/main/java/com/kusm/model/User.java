package com.kusm.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
    
    // Profile fields
    @Column(length = 500)
    private String profilePhotoUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;
    
    @Column(length = 100)
    private String city;
    
    private LocalDate dateOfBirth;
    
    // Billing address relationship - One billing address per user
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;
    
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
    @UpdateTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;

    
    
    // Enum for user status
    public enum UserStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DEACTIVATED
    }
    
    // Enum for gender
    public enum Gender {
        MALE,
        FEMALE,
        OTHER,
        PREFER_NOT_TO_SAY
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
    
    // Profile constructor
    public User(String name, String email, String phoneNumber, Gender gender, String city, LocalDate dateOfBirth) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.city = city;
        this.dateOfBirth = dateOfBirth;
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
    
    // Helper methods for profile
    public boolean hasCompleteProfile() {
        return name != null && email != null && phoneNumber != null && 
               gender != null && city != null && dateOfBirth != null;
    }
    
    // Billing address management helper methods
    public void setBillingAddress(Address address) {
        this.billingAddress = address;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean hasBillingAddress() {
        return billingAddress != null;
    }
    
    // Updated method to work with simplified Address entity
    public void updateBillingAddress(String addressLine1, String city, String state, String postalCode) {
        if (billingAddress == null) {
            billingAddress = new Address(addressLine1, city, state, postalCode);
        } else {
            billingAddress.setAddressLine1(addressLine1);
            billingAddress.setCity(city);
            billingAddress.setState(state);
            billingAddress.setPostalCode(postalCode);
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void removeBillingAddress() {
        this.billingAddress = null;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Method to update profile information
    public void updateProfile(String profilePhotoUrl, Gender gender, String city, LocalDate dateOfBirth) {
        this.profilePhotoUrl = profilePhotoUrl;
        this.gender = gender;
        this.city = city;
        this.dateOfBirth = dateOfBirth;
        this.updatedAt = LocalDateTime.now();
    }
}