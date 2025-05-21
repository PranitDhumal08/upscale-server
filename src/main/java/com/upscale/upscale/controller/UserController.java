package com.upscale.upscale.controller;

import com.upscale.upscale.dto.UserCreate;
import com.upscale.upscale.dto.UserLogin;
import com.upscale.upscale.dto.UserLoginData;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.security.JwtTokenUtil;
import com.upscale.upscale.service.EmailService;
import com.upscale.upscale.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody UserLogin user) {

        try {
            String emailId = user.getEmailId();
            if (emailId == null || emailId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email ID is required");
            }

            String otp = String.valueOf(userService.generateOtp());

            User existingUser = userService.getUser(emailId);
            Map<String, String> response = new HashMap<>();

            if (existingUser == null) {
                User newUser = new User();
                newUser.setEmailId(emailId);
                newUser.setOtp(otp);
                newUser.setNewUser(true);
                response.put("isNewUser", "true");
                userService.save(newUser);
                log.info("User created: " + emailId + "suceessfully");
            } else {
                existingUser.setOtp(otp);
                existingUser.setNewUser(false);
                userService.save(existingUser);
                response.put("isNewUser", "false");
                log.info("User updated: " + emailId + "suceessfully");
            }

            //emailService.sendOtpEmail(emailId, otp);


            response.put("message", "OTP sent successfully");
            response.put("email", emailId);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Failed to send OTP: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody UserLoginData userLoginData) {
        try {
            if (userLoginData != null) {
                String emailId = userLoginData.getEmailId();
                String otp = userLoginData.getOtp();

                if (userService.findByEmailIdAndOtp(emailId, otp)) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "OTP verified successfully");
                    response.put("email", emailId);

                    if (userService.isNewUser(emailId)) {
                        response.put("isNewUser", "true");
                    } else {
                        response.put("isNewUser", "false");
                    }

                    // Generate JWT token
                    String token = jwtTokenUtil.generateToken(emailId);
                    response.put("token", token);

                    return new ResponseEntity<>(response, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(HttpServletRequest request, @RequestBody UserCreate userCreate){
        try {

            if(userCreate == null) return new ResponseEntity<>("Enter data properly", HttpStatus.BAD_REQUEST);

            String authHeader = request.getHeader("Authorization");
            String token = "";
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Extract the token (remove "Bearer " prefix)
                token = authHeader.substring(7);
            }

            //set all data;
            String email = jwtTokenUtil.getEmailFromToken(token);

            User user = userService.getUserDetails(email, userCreate);

            userService.save(user);
            log.info("User Updated: " + email + " successfully");
            HashMap<String, Object> response = new HashMap<>();

            response.put("message", "User created successfully");
            response.put("Name", userCreate.getFullName());
            response.put("email", email);
            response.put("token", jwtTokenUtil.generateToken(email));
            return new ResponseEntity<>(response, HttpStatus.OK);

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
