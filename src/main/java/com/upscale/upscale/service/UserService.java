package com.upscale.upscale.service;

import com.upscale.upscale.dto.UserCreate;
import com.upscale.upscale.dto.UserLogin;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    private PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public boolean checkUserExists(String emailId) {
        return userRepo.findByEmailId(emailId) != null;
    }

    public int generateOtp() {
        Random random = new Random();
        return 100000 + random.nextInt(900000); // Generates a 6-digit OTP
    }



    public void save(User user) {
        userRepo.save(user);
    }

    public boolean findByEmailIdAndOtp(String emailId,String otp) {
        User user =  userRepo.findByEmailIdAndOtp( emailId, otp);
        if(user != null){
            if(user.getEmailId().equals(emailId) && user.getOtp().equals(otp)){
                return true;
            }
        }
        return false;
    }
    public boolean isNewUser(String emailId) {
        return userRepo.findByEmailId(emailId).isNewUser();
    }

    public User getUser(String emailId){
        return userRepo.findByEmailId(emailId);
    }

    public User getUserDetails(String emailId, UserCreate userCreate){

        User user = getUser(emailId);

        user.setFullName(userCreate.getFullName());
        user.setAsanaUsed(userCreate.getAsanaUsed());
        user.setRole(userCreate.getRole());
        user.setNewUser(false);
        user.setPassword(passwordEncoder.encode(userCreate.getPassword()));
        user.setWorkspaces(userCreate.getWorkspaces());
        user.setOtp("");

        return user;
    }
}
