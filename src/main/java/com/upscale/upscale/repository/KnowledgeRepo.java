package com.upscale.upscale.repository;


import com.upscale.upscale.entity.workspace.Knowledge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface KnowledgeRepo extends MongoRepository<Knowledge, String> {
    List<Knowledge> findByWorkspaceId(String id);
}
