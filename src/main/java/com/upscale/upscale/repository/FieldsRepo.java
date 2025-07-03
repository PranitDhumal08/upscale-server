package com.upscale.upscale.repository;

import com.upscale.upscale.entity.portfolio.Fields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FieldsRepo extends MongoRepository<Fields, String> {
    Fields findByProjectId(String projectId);
    List<Fields> findAllByProjectId(String projectId);
}
