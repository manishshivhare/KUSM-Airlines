package com.kusm.dto.userDTO;

import java.time.LocalDate;

import com.kusm.model.User.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Profile Update Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotNull(message = "Gender is required")
    private Gender gender;
    
    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    private String city;
    
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
}
