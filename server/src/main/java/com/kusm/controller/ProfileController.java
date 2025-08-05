package com.kusm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kusm.dto.userDTO.AddressResponse;
import com.kusm.dto.userDTO.BillingAddressRequest;
import com.kusm.dto.userDTO.ProfileUpdateRequest;
import com.kusm.dto.userDTO.response.ApiResponse;
import com.kusm.dto.userDTO.response.ProfilePhotoResponse;
import com.kusm.dto.userDTO.response.ProfileResponse;
import com.kusm.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/{userId}/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    // ========================= PROFILE MANAGEMENT =========================

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable Long userId) {
        ProfileResponse profile = profileService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        ProfileResponse updatedProfile = profileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedProfile));
    }

    // ========================= PROFILE PHOTO MANAGEMENT =========================

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfilePhotoResponse>> uploadProfilePhoto(
            @PathVariable Long userId,
            @RequestParam("photo") MultipartFile file) {
        ProfilePhotoResponse response = profileService.updateProfilePhoto(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Profile photo uploaded successfully", response));
    }

    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfilePhotoResponse>> updateProfilePhoto(
            @PathVariable Long userId,
            @RequestParam("photo") MultipartFile file) {
        ProfilePhotoResponse response = profileService.updateProfilePhoto(userId, file);
        return ResponseEntity.ok(ApiResponse.success("Profile photo updated successfully", response));
    }

    @DeleteMapping("/photo")
    public ResponseEntity<ApiResponse<String>> deleteProfilePhoto(@PathVariable Long userId) {
        String message = profileService.deleteProfilePhoto(userId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // ========================= BILLING ADDRESS MANAGEMENT =========================

    @GetMapping("/billing-address")
    public ResponseEntity<ApiResponse<AddressResponse>> getBillingAddress(@PathVariable Long userId) {
        AddressResponse address = profileService.getBillingAddress(userId);
        return ResponseEntity.ok(ApiResponse.success("Billing address retrieved successfully", address));
    }

    @PostMapping("/billing-address")
    public ResponseEntity<ApiResponse<AddressResponse>> createBillingAddress(
            @PathVariable Long userId,
            @Valid @RequestBody BillingAddressRequest request) {
        AddressResponse address = profileService.updateBillingAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Billing address created successfully", address));
    }

    @PutMapping("/billing-address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateBillingAddress(
            @PathVariable Long userId,
            @Valid @RequestBody BillingAddressRequest request) {
        AddressResponse address = profileService.updateBillingAddress(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Billing address updated successfully", address));
    }

    @DeleteMapping("/billing-address")
    public ResponseEntity<ApiResponse<String>> deleteBillingAddress(@PathVariable Long userId) {
        String message = profileService.deleteBillingAddress(userId);
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}