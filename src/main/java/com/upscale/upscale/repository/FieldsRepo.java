package com.upscale.upscale.repository;

import com.upscale.upscale.entity.portfolio.Fields;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FieldsRepo extends MongoRepository<Fields, String> {
    Fields findByProjectId(String projectId);
}
