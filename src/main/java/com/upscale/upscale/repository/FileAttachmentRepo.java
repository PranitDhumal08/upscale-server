package com.upscale.upscale.repository;

import com.upscale.upscale.entity.project.FileAttachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAttachmentRepo extends MongoRepository<FileAttachment, String> {
    List<FileAttachment> findByProjectId(String projectId);
    List<FileAttachment> findBySenderId(String senderId);
    List<FileAttachment> findByReceiverIdsContaining(String receiverId);
}
