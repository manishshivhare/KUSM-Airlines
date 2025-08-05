package com.kusm.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kusm.dto.userDTO.AddressResponse;
import com.kusm.dto.userDTO.BillingAddressRequest;
import com.kusm.dto.userDTO.ProfileUpdateRequest;
import com.kusm.dto.userDTO.response.ProfilePhotoResponse;
import com.kusm.dto.userDTO.response.ProfileResponse;
import com.kusm.exceptions.ProfileUpdateException;
import com.kusm.exceptions.UserNotFoundException;
import com.kusm.model.Address;
import com.kusm.model.User;
import com.kusm.repository.UserRepository;

@Service
@Transactional
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    public ProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return convertToProfileResponse(user);
    }

    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = findUserById(userId);

        try {
            user.setName(request.getName());
            user.setGender(request.getGender());
            user.setCity(request.getCity());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setUpdatedAt(LocalDateTime.now());

            userRepository.save(user);
            return convertToProfileResponse(user);
        } catch (Exception e) {
            throw new ProfileUpdateException("Failed to update profile: " + e.getMessage());
        }
    }

    public ProfilePhotoResponse updateProfilePhoto(Long userId, MultipartFile file) {
        User user = findUserById(userId);

        try {
            // Delete old profile photo if exists
            if (user.getProfilePhotoUrl() != null) {
                s3Service.deleteProfilePhoto(user.getProfilePhotoUrl());
            }

            // Upload new profile photo
            String photoUrl = s3Service.uploadProfilePhoto(file, userId);
            
            // Update user profile photo URL
            user.setProfilePhotoUrl(photoUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return new ProfilePhotoResponse(photoUrl);
        } catch (Exception e) {
            throw new ProfileUpdateException("Failed to update profile photo: " + e.getMessage());
        }
    }

    public String deleteProfilePhoto(Long userId) {
        User user = findUserById(userId);

        if (user.getProfilePhotoUrl() == null) {
            throw new ProfileUpdateException("No profile photo to delete");
        }

        try {
            // Delete from S3
            s3Service.deleteProfilePhoto(user.getProfilePhotoUrl());
            
            // Update user profile
            user.setProfilePhotoUrl(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return "Profile photo deleted successfully";
        } catch (Exception e) {
            throw new ProfileUpdateException("Failed to delete profile photo: " + e.getMessage());
        }
    }

    public AddressResponse updateBillingAddress(Long userId, BillingAddressRequest request) {
        User user = findUserById(userId);

        try {
            if (user.getBillingAddress() == null) {
                // Create new address
                Address address = new Address(
                    request.getAddressLine1(),
                    request.getCity(),
                    request.getState(),
                    request.getPostalCode()
                );
                user.setBillingAddress(address);
            } else {
                // Update existing address
                Address address = user.getBillingAddress();
                address.setAddressLine1(request.getAddressLine1());
                address.setCity(request.getCity());
                address.setState(request.getState());
                address.setPostalCode(request.getPostalCode());
            }

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return convertToAddressResponse(user.getBillingAddress());
        } catch (Exception e) {
            throw new ProfileUpdateException("Failed to update billing address: " + e.getMessage());
        }
    }

    public AddressResponse getBillingAddress(Long userId) {
        User user = findUserById(userId);
        
        if (user.getBillingAddress() == null) {
            throw new ProfileUpdateException("No billing address found");
        }

        return convertToAddressResponse(user.getBillingAddress());
    }

    public String deleteBillingAddress(Long userId) {
        User user = findUserById(userId);

        if (user.getBillingAddress() == null) {
            throw new ProfileUpdateException("No billing address to delete");
        }

        try {
            user.setBillingAddress(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            return "Billing address deleted successfully";
        } catch (Exception e) {
            throw new ProfileUpdateException("Failed to delete billing address: " + e.getMessage());
        }
    }

    // Helper methods
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    private ProfileResponse convertToProfileResponse(User user) {
        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setProfilePhotoUrl(user.getProfilePhotoUrl());
        response.setGender(user.getGender());
        response.setCity(user.getCity());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setHasCompleteProfile(user.hasCompleteProfile());

        if (user.getBillingAddress() != null) {
            response.setBillingAddress(convertToAddressResponse(user.getBillingAddress()));
        }

        return response;
    }

    private AddressResponse convertToAddressResponse(Address address) {
        if (address == null) return null;
        
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setAddressLine1(address.getAddressLine1());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setPostalCode(address.getPostalCode());
        response.setFullAddress(address.getFullAddress());
        return response;
    }
}