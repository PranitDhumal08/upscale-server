package com.upscale.upscale.repository;

import com.upscale.upscale.entity.project.Inbox;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InboxRepo extends MongoRepository<Inbox, String> {

    List<Inbox> findByReceiverId(String receiverId);
    List<Inbox> findByReceiverIdAndType(String receiverId, String type);
}
