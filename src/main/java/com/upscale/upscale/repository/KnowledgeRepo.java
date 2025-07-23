package com.upscale.upscale.repository;


import com.upscale.upscale.entity.workspace.Knowledge;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface KnowledgeRepo extends MongoRepository<Knowledge, String> {
    Knowledge findByWorkspaceId(String id);
}
