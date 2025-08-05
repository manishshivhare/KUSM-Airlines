package com.kusm.dto.userDTO.response;

import java.time.LocalDate;

import com.kusm.dto.userDTO.AddressResponse;
import com.kusm.model.User.Gender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePhotoUrl;
    private Gender gender;
    private String city;
    private LocalDate dateOfBirth;
    private AddressResponse billingAddress;
    private Boolean hasCompleteProfile;
}
