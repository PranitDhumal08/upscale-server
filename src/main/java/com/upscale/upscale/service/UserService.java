package com.upscale.upscale.service;

import com.upscale.upscale.dto.LoginUser;
import com.upscale.upscale.dto.UserCreate;
import com.upscale.upscale.dto.UserLogin;
import com.upscale.upscale.entity.Project;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    private PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public UserService() {
        // Set default password encoder to bcrypt
        System.setProperty("spring.security.password.encoder", "bcrypt");
    }

    public boolean checkUserExists(String emailId) {
        return userRepo.findByEmailId(emailId) != null;
    }

    public int generateOtp() {
        Random random = new Random();
        return 100000 + random.nextInt(900000); // Generates a 6-digit OTP
    }

    public boolean login(LoginUser loginUser) {
        User user = userRepo.findByEmailId(loginUser.getEmail());
        if(user != null) {
            if(passwordEncoder.matches(loginUser.getPassword(),user.getPassword())) return true;
        }
        return false;
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

    public String getName(String emailId){
        User user = getUser(emailId);
        if(user != null) return user.getFullName();

        return "";
    }
    public String getDate(){
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d");
        return today.format(formatter);
    }
    public boolean setProject(Project project,String emailId){
        User user = getUser(emailId);
        if(user != null){
            user.getProjects().add(project);
            save(user);
            return true;
        }
        return false;
    }

    public List<String> getTeamMates(String emailId) {
        User user = getUser(emailId);;
        if(user != null){
            return user.getTeammates();
        }
        return new ArrayList<>();
    }

    public HashMap<String,String> getProjects(String emailId) {

        HashMap<String,String> map = new HashMap<>();

        User user = getUser(emailId);
        if(user != null){
            List<Project> projects = user.getProjects();
            for(Project project : projects){
                map.put(project.getId(),project.getProjectName());
            }

            return map;
        }
        return map;
    }

    public User getUserById(String id) {
        return userRepo.findById(id).orElse(null);
    }
}
