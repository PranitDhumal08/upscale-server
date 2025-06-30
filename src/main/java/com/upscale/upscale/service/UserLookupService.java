package com.upscale.upscale.service;

import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserLookupService {
    @Autowired
    private UserRepo userRepo;

    public User getUserByEmail(String emailId) {
        return userRepo.findByEmailId(emailId);
    }

    public User getUserById(String id) {
        return userRepo.findById(id).orElse(null);
    }
} 