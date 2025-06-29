package com.upscale.upscale.service.project;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your OTP for Upscale");
        message.setText("Your OTP is: " + otp + "\nThis OTP is valid for 5 minutes.");
        
        // For now, just log the email details
        log.info("Sending OTP email to: " + to);
        log.info("OTP: " + otp);
        
        // Uncomment the following line to actually send the email
        // emailSender.send(message);
    }
} 