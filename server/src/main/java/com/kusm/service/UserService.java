package com.kusm.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusm.dto.userDTO.LoginRequest;
import com.kusm.dto.userDTO.OtpVerificationRequest;
import com.kusm.dto.userDTO.SignUpRequest;
import com.kusm.dto.userDTO.response.LoginResponse;
import com.kusm.dto.userDTO.response.UserResponse;
import com.kusm.exceptions.InvalidOtpException;
import com.kusm.exceptions.MaxOtpAttemptsExceededException;
import com.kusm.exceptions.OtpExpiredException;
import com.kusm.exceptions.UserAlreadyExistsException;
import com.kusm.exceptions.UserNotFoundException;
import com.kusm.model.User;
import com.kusm.model.User.UserStatus;
import com.kusm.repository.UserRepository;
import com.kusm.utils.JwtUtil;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private JwtUtil jwtUtil;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_OTP_ATTEMPTS = 3;

    public String signUpUser(SignUpRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("User with this phone number already exists");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setStatus(UserStatus.PENDING);
        user.setIsVerified(false);
        user.setIsActive(true);

        // Generate and set OTP
        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setOtpAttempts(0);

        // Save user
        userRepository.save(user);

        // Send OTP
        otpService.sendOtpViaEmail(user.getEmail(), otp);
        if (user.getPhoneNumber() != null) {
            otpService.sendOtpViaSms(user.getPhoneNumber(), otp);
        }

        return "User registered successfully. Please verify your account with the OTP sent to your email/phone.";
    }

    public String loginUser(LoginRequest request) {
        User user = findUserByIdentifier(request.getIdentifier());

        if (!user.getIsVerified()) {
            throw new UserNotFoundException("Account not verified. Please complete verification first.");
        }

        if (!user.getIsActive() || user.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotFoundException("Account is inactive or suspended.");
        }

        // Generate and send OTP for login
        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setOtpAttempts(0);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Send OTP
        if (isEmail(request.getIdentifier())) {
            otpService.sendOtpViaEmail(request.getIdentifier(), otp);
        } else {
            otpService.sendOtpViaSms(request.getIdentifier(), otp);
        }

        return "OTP sent successfully. Please verify to complete login.";
    }

    public LoginResponse verifyOtp(OtpVerificationRequest request) {
        User user = findUserByIdentifier(request.getIdentifier());

        // Check if max attempts exceeded
        if (user.isMaxOtpAttemptsReached()) {
            throw new MaxOtpAttemptsExceededException(
                    "Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        // Check if OTP is expired
        if (user.isOtpExpired()) {
            throw new OtpExpiredException("OTP has expired. Please request a new OTP.");
        }

        // Verify OTP
        if (!user.isOtpValid(request.getOtp())) {
            user.incrementOtpAttempts();
            userRepository.save(user);
            throw new InvalidOtpException(
                    String.format("Invalid OTP. %d attempts remaining.",
                            MAX_OTP_ATTEMPTS - user.getOtpAttempts()));
        }

        // OTP is valid - complete verification/login
        if (!user.getIsVerified()) {
            // First time verification (signup)
            user.setIsVerified(true);
            user.setStatus(UserStatus.ACTIVE);
        }

        // Update login time and clear OTP data
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        clearOtpData(user);

        userRepository.save(user);

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        UserResponse userResponse = convertToUserResponse(user);

        return new LoginResponse(token, userResponse);
    }

    public String resendOtp(String identifier) {
        User user = findUserByIdentifier(identifier);

        // Generate new OTP
        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        user.setOtpAttempts(0); // Reset attempts
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // Send OTP
        if (isEmail(identifier)) {
            otpService.sendOtpViaEmail(identifier, otp);
        } else {
            otpService.sendOtpViaSms(identifier, otp);
        }

        return "New OTP sent successfully.";
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("User not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return convertToUserResponse(user);
    }

    public String logoutUser() {
        // In JWT, logout is typically handled on client side by removing token
        // But we can add token blacklisting logic here if needed
        return "Logged out successfully";
    }

    // Helper methods
    private User findUserByIdentifier(String identifier) {
        Optional<User> user;
        if (isEmail(identifier)) {
            user = userRepository.findByEmail(identifier);
        } else {
            user = userRepository.findByPhoneNumber(identifier);
        }

        return user.orElseThrow(()
                -> new UserNotFoundException("User not found with provided identifier"));
    }

    private boolean isEmail(String identifier) {
        return identifier.contains("@");
    }

    private void clearOtpData(User user) {
        user.setOtp(null);
        user.setOtpGeneratedAt(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setIsVerified(user.getIsVerified());
        response.setIsActive(user.getIsActive());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
    }
}
