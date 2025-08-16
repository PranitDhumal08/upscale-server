package com.upscale.upscale.repository;

import com.upscale.upscale.entity.project.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepo extends MongoRepository<Message, String> {
    List<Message> findByRecipientsContaining(String attr0);
    List<Message> findByProjectId(String projectId);
}
