package com.kusm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Address line 1 is required")
    @Column(nullable = false, length = 200)
    private String addressLine1;
    
    @NotBlank(message = "City is required")
    @Column(nullable = false, length = 100)
    private String city;
    
    @NotBlank(message = "State is required")
    @Column(nullable = false, length = 100)
    private String state;
    
    @NotBlank(message = "Postal code is required")
    @Size(min = 3, max = 20, message = "Postal code must be between 3 and 20 characters")
    @Column(nullable = false, length = 20)
    private String postalCode;
    
    // Constructor for basic address
    public Address(String addressLine1, String city, String state, String postalCode) {
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
    }
    
    // Helper method to get full address
    public String getFullAddress() {
        return addressLine1 + ", " + city + ", " + state + " " + postalCode;
    }
}