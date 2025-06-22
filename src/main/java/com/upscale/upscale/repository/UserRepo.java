package com.upscale.upscale.repository;

import com.upscale.upscale.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepo extends MongoRepository<User, String> {
    User findByEmailId(String emailId);
    User findByEmailIdAndOtp(String emailId,String otp);
}
