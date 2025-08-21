package com.upscale.upscale.repository;

import com.upscale.upscale.entity.project.SubTask;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubTaskRepo extends MongoRepository<SubTask, String> {
    // Matches subtasks where the assignId array contains the given user id
    java.util.List<SubTask> findByAssignId(String userId);
}
