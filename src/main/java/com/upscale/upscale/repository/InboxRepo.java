package com.upscale.upscale.repository;

import com.upscale.upscale.entity.project.Inbox;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface InboxRepo extends MongoRepository<Inbox, String> {

    List<Inbox> findByReceiverId(String receiverId);
    List<Inbox> findByReceiverIdAndType(String receiverId, String type);

    // Archived-aware queries
    List<Inbox> findByReceiverIdAndArchivedFalse(String receiverId);
    List<Inbox> findByReceiverIdAndTypeAndArchivedFalse(String receiverId, String type);
    List<Inbox> findByReceiverIdAndArchivedTrue(String receiverId);
    List<Inbox> findByReceiverIdAndArchivedTrueAndType(String receiverId, String type);

    // List-based queries (to support receiver stored as email or userId)
    List<Inbox> findByReceiverIdInAndArchivedFalse(List<String> receiverIds);
    List<Inbox> findByReceiverIdInAndArchivedTrue(List<String> receiverIds);
    List<Inbox> findByReceiverIdInAndTypeAndArchivedFalse(List<String> receiverIds, String type);
    List<Inbox> findByReceiverIdInAndTypeAndArchivedTrue(List<String> receiverIds, String type);

    // Email-based queries that treat missing 'archived' as false
    @Query(value = "{ 'receiverId': ?0, $or: [ { 'archived': false }, { 'archived': { $exists: false } } ] }")
    List<Inbox> findActiveByReceiverEmail(String receiverEmail);

    @Query(value = "{ 'receiverId': ?0, 'type': ?1, $or: [ { 'archived': false }, { 'archived': { $exists: false } } ] }")
    List<Inbox> findActiveByReceiverEmailAndType(String receiverEmail, String type);
}
