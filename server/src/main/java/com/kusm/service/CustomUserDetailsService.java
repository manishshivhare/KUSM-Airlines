package com.kusm.service;

import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.kusm.model.User;
import com.kusm.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt;
        
        // Check if username is email or phone number
        if (username.contains("@")) {
            userOpt = userRepository.findByEmail(username);
        } else {
            userOpt = userRepository.findByPhoneNumber(username);
        }

        User user = userOpt.orElseThrow(() -> 
            new UsernameNotFoundException("User not found with identifier: " + username));

        // Check if user is verified and active
        if (!user.getIsVerified()) {
            throw new UsernameNotFoundException("User account is not verified");
        }

        if (!user.getIsActive() || user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("User account is inactive");
        }

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(), // Using email as username for consistency
            "", // No password since we use OTP
            true, // enabled
            true, // accountNonExpired
            true, // credentialsNonExpired
            true, // accountNonLocked
            new ArrayList<>() // authorities - empty for now, can add roles later
        );
    }

    public User findUserByIdentifier(String identifier) {
        Optional<User> userOpt;
        
        if (identifier.contains("@")) {
            userOpt = userRepository.findByEmail(identifier);
        } else {
            userOpt = userRepository.findByPhoneNumber(identifier);
        }

        return userOpt.orElseThrow(() -> 
            new UsernameNotFoundException("User not found with identifier: " + identifier));
    }
}