package com.upscale.upscale.repository;

import com.upscale.upscale.entity.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface GoalRepo extends MongoRepository<Goal, String> {

    List<Goal> findFirstByUserId(String userId);
    List<Goal> findByUserId(String userId);
}
