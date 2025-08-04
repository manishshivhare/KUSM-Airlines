package com.kusm.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private static final SecureRandom random = new SecureRandom();

    @Autowired
    private JavaMailSender mailSender;

    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public void sendOtpViaEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Code");
        message.setText("Your OTP is: " + otp);
        message.setFrom("yourgmail@gmail.com");

        mailSender.send(message);
        System.out.println("OTP " + otp + " sent to email: " + email);
    }

    public void sendOtpViaSms(String phoneNumber, String otp) {
        // Stub method – integrate with SMS API
        System.out.println("Sending OTP " + otp + " to phone: " + phoneNumber);
    }
}
