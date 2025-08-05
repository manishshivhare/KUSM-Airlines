package com.kusm.dto.userDTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePhotoResponse {
    private String profilePhotoUrl;
    private String message;
    
    public ProfilePhotoResponse(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
        this.message = "Profile photo updated successfully";
    }
}