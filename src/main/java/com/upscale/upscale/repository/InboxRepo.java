package com.upscale.upscale.repository;

import com.upscale.upscale.entity.Inbox;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InboxRepo extends MongoRepository<Inbox, String> {

    Inbox findByReceiverId(String senderId);
}
