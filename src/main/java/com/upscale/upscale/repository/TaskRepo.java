package com.upscale.upscale.repository;

import com.upscale.upscale.entity.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepo extends MongoRepository<Task, String> {

    List<Task> findByCreatedId(String createdId);


}
