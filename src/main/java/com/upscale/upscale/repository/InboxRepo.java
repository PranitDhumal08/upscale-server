package com.upscale.upscale.repository;

import com.upscale.upscale.entity.Inbox;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InboxRepo extends MongoRepository<Inbox, String> {

    List<Inbox> findByReceiverId(String receiverId);
}
