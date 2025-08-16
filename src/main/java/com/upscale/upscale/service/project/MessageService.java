package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.MessageData;
import com.upscale.upscale.entity.project.Message;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.MessageRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MessageService {

    @Autowired
    private MessageRepo messageRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private ProjectService projectService;

    public boolean sendMessage(String emailId, MessageData messageData) {

        if (messageData == null) {
            return false;
        }

        User user = userService.getUser(emailId);

        if (user == null) {
            return false;
        }

        Message message = new Message();
        message.setSender(user.getId());
        message.setSubject(messageData.getSubject());
        message.setBody(messageData.getBody());
        message.setProjectId(messageData.getProjectId()); // Set project context
        
        List<String> validRecipients = new ArrayList<>();
        
        // If recipients is empty, send to all project members
        if (messageData.getRecipients() == null || messageData.getRecipients().isEmpty()) {
            log.info("Recipients list is empty, sending message to all project members");
            
            if (messageData.getProjectId() != null && !messageData.getProjectId().isEmpty()) {
                try {
                    // Get project and all its members
                    com.upscale.upscale.entity.project.Project project = projectService.getProject(messageData.getProjectId());
                    
                    if (project != null) {
                        // Add project owner
                        if (project.getUserEmailid() != null && !project.getUserEmailid().equals(emailId)) {
                            validRecipients.add(project.getUserEmailid());
                            inboxService.sendProjectMessage(message, emailId, project.getUserEmailid());
                        }
                        
                        // Add all teammates from the project
                        if (project.getTeammates() != null) {
                            for (java.util.Map.Entry<String, String[]> entry : project.getTeammates().entrySet()) {
                                String[] teammateInfo = entry.getValue();
                                if (teammateInfo.length > 2) {
                                    String teammateEmail = teammateInfo[2]; // Email is at index 2
                                    if (teammateEmail != null && teammateEmail.contains("@") && !teammateEmail.equals(emailId)) {
                                        validRecipients.add(teammateEmail);
                                        inboxService.sendProjectMessage(message, emailId, teammateEmail);
                                    }
                                }
                            }
                        }
                        
                        log.info("Message sent to all {} project members", validRecipients.size());
                    } else {
                        log.warn("Project not found with ID: {}", messageData.getProjectId());
                        return false;
                    }
                } catch (Exception e) {
                    log.error("Error getting project members for project {}: {}", messageData.getProjectId(), e.getMessage());
                    return false;
                }
            } else {
                log.warn("Project ID is required when recipients list is empty");
                return false;
            }
        } else {
            // Process specific recipients - all should be email addresses
            for (String recipientEmail : messageData.getRecipients()) {
                // Validate that it's an email address
                if (recipientEmail != null && recipientEmail.contains("@")) {
                    validRecipients.add(recipientEmail);
                    // Send inbox notification to each recipient
                    inboxService.sendProjectMessage(message, emailId, recipientEmail);
                } else {
                    log.warn("Invalid recipient email format: {}", recipientEmail);
                }
            }
        }
        
        message.setRecipients(validRecipients);
        messageRepo.save(message);

        log.info("Message sent from {} to {} recipients in project {}", 
                emailId, validRecipients.size(), messageData.getProjectId());

        return true;
    }

    public List<Message> getMessagesForUser(String projectId) {
        // Find messages by project ID
        List<Message> messages = messageRepo.findByProjectId(projectId);
        log.info("Found {} messages for project {}", messages.size(), projectId);
        return messages;
    }

    public List<java.util.Map<String, Object>> getEnrichedMessagesForProject(String projectId) {
        // Find messages by project ID
        List<Message> messages = messageRepo.findByProjectId(projectId);
        List<java.util.Map<String, Object>> enrichedMessages = new ArrayList<>();
        
        for (Message message : messages) {
            java.util.Map<String, Object> enrichedMessage = new java.util.HashMap<>();
            
            // Copy all original message fields
            enrichedMessage.put("id", message.getId());
            enrichedMessage.put("subject", message.getSubject());
            enrichedMessage.put("body", message.getBody());
            enrichedMessage.put("sender", message.getSender()); // Keep original sender ID
            enrichedMessage.put("recipients", message.getRecipients());
            enrichedMessage.put("projectId", message.getProjectId());
            
            // Add sender name by looking up user information
            try {
                User senderUser = userService.getUserById(message.getSender());
                if (senderUser != null) {
                    enrichedMessage.put("senderName", senderUser.getFullName());
                    enrichedMessage.put("senderEmail", senderUser.getEmailId());
                } else {
                    enrichedMessage.put("senderName", "Unknown User");
                    enrichedMessage.put("senderEmail", "unknown@example.com");
                    log.warn("Sender user not found for ID: {}", message.getSender());
                }
            } catch (Exception e) {
                enrichedMessage.put("senderName", "Unknown User");
                enrichedMessage.put("senderEmail", "unknown@example.com");
                log.error("Error looking up sender user {}: {}", message.getSender(), e.getMessage());
            }
            
            enrichedMessages.add(enrichedMessage);
        }
        
        log.info("Enriched {} messages with sender names for project {}", enrichedMessages.size(), projectId);
        return enrichedMessages;
    }

    public List<Message> getMessagesForUserEmail(String email) {
        // Find messages where user is a recipient (by email)
        List<Message> messages = messageRepo.findByRecipientsContaining(email);
        log.info("Found {} messages for user email {}", messages.size(), email);
        return messages;
    }
}
