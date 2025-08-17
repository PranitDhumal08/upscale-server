package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.MessageData;
import com.upscale.upscale.entity.project.Message;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.workspace.Workspace;
import com.upscale.upscale.repository.MessageRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import com.upscale.upscale.service.Workspace.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private WorkspaceService workspaceService;

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
        
        // If recipients is empty, send to all members with fallback logic: Project -> Portfolio -> Workspace
        if (messageData.getRecipients() == null || messageData.getRecipients().isEmpty()) {
            log.info("Recipients list is empty, attempting to find members using fallback logic");
            
            if (messageData.getProjectId() != null && !messageData.getProjectId().isEmpty()) {
                try {
                    // Step 1: Try to find as Project
                    com.upscale.upscale.entity.project.Project project = projectService.getProject(messageData.getProjectId());
                    
                    if (project != null) {
                        log.info("Found as Project: {}", project.getProjectName());
                        
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
                        // Step 2: Try to find as Portfolio
                        log.info("Project not found, trying as Portfolio with ID: {}", messageData.getProjectId());
                        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolio(messageData.getProjectId());
                        
                        if (portfolioOpt.isPresent()) {
                            Portfolio portfolio = portfolioOpt.get();
                            log.info("Found as Portfolio: {}", portfolio.getPortfolioName());
                            
                            // Add portfolio owner
                            User portfolioOwner = userService.getUserById(portfolio.getOwnerId());
                            if (portfolioOwner != null && !portfolioOwner.getEmailId().equals(emailId)) {
                                validRecipients.add(portfolioOwner.getEmailId());
                                inboxService.sendProjectMessage(message, emailId, portfolioOwner.getEmailId());
                            }
                            
                            // Add all teammates from the portfolio
                            if (portfolio.getTeammates() != null) {
                                for (String teammateId : portfolio.getTeammates()) {
                                    User teammate = userService.getUserById(teammateId);
                                    if (teammate != null && !teammate.getEmailId().equals(emailId)) {
                                        validRecipients.add(teammate.getEmailId());
                                        inboxService.sendProjectMessage(message, emailId, teammate.getEmailId());
                                    }
                                }
                            }
                            
                            log.info("Message sent to all {} portfolio members", validRecipients.size());
                        } else {
                            // Step 3: Try to find as Workspace
                            log.info("Portfolio not found, trying as Workspace with ID: {}", messageData.getProjectId());
                            
                            // First try to get workspace by ID directly
                            Workspace workspace = null;
                            try {
                                // Since WorkspaceService doesn't have getById, we need to find by user
                                // Let's try to find workspace that contains this ID in some way
                                User currentUser = userService.getUser(emailId);
                                if (currentUser != null) {
                                    workspace = workspaceService.getWorkspace(currentUser.getId());
                                }
                            } catch (Exception e) {
                                log.warn("Error getting workspace: {}", e.getMessage());
                            }
                            
                            if (workspace != null) {
                                log.info("Found Workspace: {}", workspace.getName());
                                
                                // Add workspace owner (userId)
                                User workspaceOwner = userService.getUserById(workspace.getUserId());
                                if (workspaceOwner != null && !workspaceOwner.getEmailId().equals(emailId)) {
                                    validRecipients.add(workspaceOwner.getEmailId());
                                    inboxService.sendProjectMessage(message, emailId, workspaceOwner.getEmailId());
                                }
                                
                                // Add all members from the workspace
                                if (workspace.getMembers() != null) {
                                    for (String memberId : workspace.getMembers()) {
                                        User member = userService.getUserById(memberId);
                                        if (member != null && !member.getEmailId().equals(emailId)) {
                                            validRecipients.add(member.getEmailId());
                                            inboxService.sendProjectMessage(message, emailId, member.getEmailId());
                                        }
                                    }
                                }
                                
                                log.info("Message sent to all {} workspace members", validRecipients.size());
                            } else {
                                log.warn("No Project, Portfolio, or Workspace found with ID: {}", messageData.getProjectId());
                                return false;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Error getting members for ID {}: {}", messageData.getProjectId(), e.getMessage());
                    return false;
                }
            } else {
                log.warn("Project/Portfolio/Workspace ID is required when recipients list is empty");
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

        log.info("Message sent from {} to {} recipients for ID {}", 
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
