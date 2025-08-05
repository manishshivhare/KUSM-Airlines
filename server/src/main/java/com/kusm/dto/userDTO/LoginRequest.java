package com.kusm.dto.userDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email or phone number is required")
    private String identifier; // Can be email or phone number
}
