package com.kusm.dto.userDTO;

import org.hibernate.validator.constraints.NotBlank;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingAddressRequest {
    
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200, message = "Address line 1 cannot exceed 200 characters")
    private String addressLine1;
    
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;
    
    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;
    
    @NotBlank(message = "Postal code is required")
    @Size(min = 3, max = 20, message = "Postal code must be between 3 and 20 characters")
    private String postalCode;
}
