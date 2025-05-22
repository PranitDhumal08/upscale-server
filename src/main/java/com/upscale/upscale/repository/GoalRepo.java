package com.upscale.upscale.repository;

import com.upscale.upscale.entity.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GoalRepo extends MongoRepository<Goal, String> {

    Goal findByUserId(String name);
}
