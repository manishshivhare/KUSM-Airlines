package com.kusm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kusm.dto.ApiResponse;
import com.kusm.dto.LoginRequest;
import com.kusm.dto.OtpVerificationRequest;
import com.kusm.dto.SignUpRequest;
import com.kusm.dto.UserResponse;
import com.kusm.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("User service is running"));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signUp(@Valid @RequestBody SignUpRequest request) {
        String message = userService.signUpUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest request) {
        String message = userService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<UserResponse>> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        UserResponse userResponse = userService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", userResponse));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestBody String identifier) {
        String message = userService.resendOtp(identifier.trim().replace("\"", ""));
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}