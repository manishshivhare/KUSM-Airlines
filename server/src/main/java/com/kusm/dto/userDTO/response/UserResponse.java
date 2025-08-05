// UserResponse.java
package com.kusm.dto.userDTO.response;

import java.time.LocalDateTime;

import com.kusm.model.User.UserStatus;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Boolean isVerified;
    private Boolean isActive;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
