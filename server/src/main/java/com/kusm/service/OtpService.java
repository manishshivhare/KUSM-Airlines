package com.kusm.service;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

/**
 * Simplified OTP Service with email and Twilio SMS support
 */
@Service
public class OtpService {
    
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.mail.from:noreply@yourcompany.com}")
    private String fromEmail;
    
    @Value("${twilio.account.sid}")
    private String twilioAccountSid;
    
    @Value("${twilio.auth.token}")
    private String twilioAuthToken;
    
    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    /**
     * Generates a 6-digit OTP
     */
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Sends OTP via email
     */
    public void sendOtpViaEmail(String email, String otp) {
        validateEmail(email);
        validateOtp(otp);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setFrom(fromEmail);
            message.setSubject("Your OTP Code");
            message.setText(buildEmailMessage(otp));
            
            mailSender.send(message);
            logger.info("OTP sent to email: {}", maskEmail(email));
            
        } catch (Exception e) {
            logger.error("Failed to send email OTP to: {}", maskEmail(email), e);
            throw new OtpException("Failed to send email OTP", e);
        }
    }

    /**
     * Sends OTP via Twilio SMS
     */
    public void sendOtpViaSms(String phoneNumber, String otp) {
        validatePhone(phoneNumber);
        validateOtp(otp);
        
        try {
            // Initialize Twilio
            Twilio.init(twilioAccountSid, twilioAuthToken);
            
            // Send SMS
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    buildSmsMessage(otp)
            ).create();
            
            logger.info("SMS OTP sent to: {} with SID: {}", maskPhone(phoneNumber), message.getSid());
            
        } catch (Exception e) {
            logger.error("Failed to send SMS OTP to: {}", maskPhone(phoneNumber), e);
            throw new OtpException("Failed to send SMS OTP", e);
        }
    }
    
    /**
     * Validates email format
     */
    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    /**
     * Validates phone number format
     */
    private void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        String cleanPhone = phone.replaceAll("\\s+", "");
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
    
    /**
     * Validates OTP format
     */
    private void validateOtp(String otp) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP must be a 6-digit number");
        }
    }
    
    /**
     * Builds email message body
     */
    private String buildEmailMessage(String otp) {
        return String.format(
            "Your verification code is: %s\n\n" +
            "This code expires in 10 minutes.\n" +
            "Do not share this code with anyone.",
            otp
        );
    }
    
    /**
     * Builds SMS message body
     */
    private String buildSmsMessage(String otp) {
        return String.format("Your verification code is: %s. Expires in 10 minutes.", otp);
    }
    
    /**
     * Masks email for logging
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        return (local.length() > 3 ? local.substring(0, 3) : local) + "***@" + parts[1];
    }
    
    /**
     * Masks phone number for logging
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        String digits = phone.replaceAll("[^0-9]", "");
        return "****" + digits.substring(Math.max(0, digits.length() - 4));
    }

    /**
     * Custom exception for OTP operations
     */
    public static class OtpException extends RuntimeException {
        public OtpException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}