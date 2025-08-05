package com.kusm.dto.userDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String addressLine1;
    private String city;
    private String state;
    private String postalCode;
    private String fullAddress;
}
