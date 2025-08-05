package com.kusm.dto.userDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerificationRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier; // email or phone
    
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    @NotBlank(message = "OTP is required")
    private String otp;
}