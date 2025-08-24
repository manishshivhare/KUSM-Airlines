package com.kusm.dto.userDTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private UserResponse user;
    
    public LoginResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }
}