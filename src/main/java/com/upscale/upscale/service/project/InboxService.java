package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.InboxData;
import com.upscale.upscale.entity.Goal;
import com.upscale.upscale.entity.project.*;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.InboxRepo;
import com.upscale.upscale.repository.MessageRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class InboxService {

    @Autowired
    private InboxRepo inboxRepo;

    @Autowired
    private MessageRepo messageRepo;

    public void saveInbox(Inbox inbox){
        inboxRepo.save(inbox);
    }

    public void updateInbox(String emailId, Inbox inbox){
        inboxRepo.save(inbox);
    }

    public void sendInviteInbox(String senderEmailId, String receiverEmailId, People people){
        String context = "You have invite for the projects" + people.getProjectsName();
        buildAndSaveInbox("PEOPLE_INVITE", context, senderEmailId, receiverEmailId, null);
    }

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    @Lazy
    private ProjectService projectService;

    @Autowired
    @Lazy
    private TaskService taskService;

    public void sendProjectInvite(String senderEmailId, String receiverEmailId, Project project, User user) {
        String context = String.format("You have been invited to join the project '%s' in workspace '%s'",
            project.getProjectName(),
            project.getWorkspace());
        buildAndSaveInbox("PROJECT_INVITE", context, senderEmailId, receiverEmailId, project.getId());

        //user.getProjects().add(project.getId());

        List<String> projectIds = user.getProjects();
        projectIds.add(project.getId());

        user.setProjects(projectIds);

        userService.save(user);

    }

    public List<InboxData> getInbox(String emailId){
        return getInbox(emailId, null);
    }

    public List<InboxData> getInbox(String emailId, String type){
        // Query strictly by receiver email from token. Treat missing 'archived' as active.
        List<Inbox> inboxes = (type == null || type.isBlank())
                ? inboxRepo.findActiveByReceiverEmail(emailId)
                : inboxRepo.findActiveByReceiverEmailAndType(emailId, type);

        List<InboxData> inboxDataList = new ArrayList<>();

        if(inboxes != null && !inboxes.isEmpty()){
            for (Inbox inbox : inboxes) {
                InboxData inboxData = new InboxData();

                inboxData.setId(inbox.getId());
                inboxData.setSenderId(inbox.getSenderId());
                inboxData.setReceiverId(inbox.getReceiverId());
                // Build a user-friendly content message based on type and related entity
                String friendly = buildFriendlyContent(inbox);
                inboxData.setContent(friendly != null ? friendly : inbox.getContent());
                inboxData.setType(inbox.getType());
                inboxData.setEntityId(inbox.getEntityId());
                inboxData.setCreatedAt(inbox.getCreatedAt());
                inboxData.setRead(inbox.isRead());
                inboxData.setArchived(inbox.isArchived());

                inboxDataList.add(inboxData);
            }
            return inboxDataList;
        }
        return new ArrayList<>();
    }

    public void sendTaskDetails(Task task, String senderEmailId, String receiverEmailId){
        String context = "You have been assigned a task: " + task.getTaskName();
        buildAndSaveInbox("TASK_ASSIGNED", context, senderEmailId, receiverEmailId, task.getId());
    }

    public void sendProjectMessage(Message message, String senderEmailId, String receiverEmailId){
        buildAndSaveInbox("PROJECT_MESSAGE", message.getBody(), senderEmailId, receiverEmailId, message.getId());
    }

    public void sendGoalMessage(Goal goal, String senderEmailId, String receiverEmailId){
        buildAndSaveInbox("GOAL_INVITE", "You have been added to a goal", senderEmailId, receiverEmailId, goal.getId());
    }

    // Send an inbox message to the sender themselves (self-confirmation)
    public void sendSelf(String type, String content, String senderEmailId, String entityId) {
        buildAndSaveInbox(type, content, senderEmailId, senderEmailId, entityId);
    }

    private void buildAndSaveInbox(String type, String content, String senderEmailId, String receiverEmailId, String entityId) {
        try {
            Inbox inbox = new Inbox();
            inbox.setSenderId(senderEmailId);
            inbox.setReceiverId(receiverEmailId);
            inbox.setContent(content);
            inbox.setType(type);
            inbox.setEntityId(entityId);
            inbox.setCreatedAt(new Date());
            inbox.setRead(false);
            inbox.setArchived(false);
            saveInbox(inbox);
        } catch (Exception e) {
            log.error("Failed to save inbox message type {} to {}: {}", type, receiverEmailId, e.getMessage());
        }
    }

    public boolean markRead(String inboxId, String receiverEmail) {
        try {
            java.util.Optional<Inbox> opt = inboxRepo.findById(inboxId);
            if (opt.isEmpty()) return false;
            Inbox inbox = opt.get();
            // Authorize: requester matches by email or by userId
            String requesterUserId = null;
            try { User u = userService.getUser(receiverEmail); requesterUserId = (u != null ? u.getId() : null); } catch (Exception ignored) {}
            boolean authorized = receiverEmail.equals(inbox.getReceiverId()) || (requesterUserId != null && requesterUserId.equals(inbox.getReceiverId()));
            if (!authorized) {
                log.warn("markRead denied: requester {} not receiver of inbox {}", receiverEmail, inboxId);
                return false; // prevent others marking it
            }
            if (!inbox.isRead()) {
                inbox.setRead(true);
                inboxRepo.save(inbox);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to mark inbox {} as read: {}", inboxId, e.getMessage());
            return false;
        }
    }

    public boolean archive(String inboxId, String receiverEmail) {
        try {
            Optional<Inbox> opt = inboxRepo.findById(inboxId);
            if (opt.isEmpty()) return false;
            Inbox inbox = opt.get();
            String requesterUserId = null;
            try { User u = userService.getUser(receiverEmail); requesterUserId = (u != null ? u.getId() : null); } catch (Exception ignored) {}
            boolean authorized = receiverEmail.equals(inbox.getReceiverId()) || (requesterUserId != null && requesterUserId.equals(inbox.getReceiverId()));
            if (!authorized) {
                log.warn("archive denied: requester {} not receiver of inbox {}", receiverEmail, inboxId);
                return false;
            }
            if (!inbox.isArchived()) {
                inbox.setArchived(true);
                inboxRepo.save(inbox);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to archive inbox {}: {}", inboxId, e.getMessage());
            return false;
        }
    }

    public boolean unarchive(String inboxId, String receiverEmail) {
        try {
            Optional<Inbox> opt = inboxRepo.findById(inboxId);
            if (opt.isEmpty()) return false;
            Inbox inbox = opt.get();
            String requesterUserId = null;
            try { User u = userService.getUser(receiverEmail); requesterUserId = (u != null ? u.getId() : null); } catch (Exception ignored) {}
            boolean authorized = receiverEmail.equals(inbox.getReceiverId()) || (requesterUserId != null && requesterUserId.equals(inbox.getReceiverId()));
            if (!authorized) {
                log.warn("unarchive denied: requester {} not receiver of inbox {}", receiverEmail, inboxId);
                return false;
            }
            if (inbox.isArchived()) {
                inbox.setArchived(false);
                inboxRepo.save(inbox);
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to unarchive inbox {}: {}", inboxId, e.getMessage());
            return false;
        }
    }

    public List<InboxData> getArchivedInbox(String emailId, String type) {
        List<String> receiverIds = new ArrayList<>();
        receiverIds.add(emailId);
        try {
            User u = userService.getUser(emailId);
            if (u != null && u.getId() != null) receiverIds.add(u.getId());
        } catch (Exception ignored) {}

        List<Inbox> inboxes = (type == null || type.isBlank())
                ? inboxRepo.findByReceiverIdInAndArchivedTrue(receiverIds)
                : inboxRepo.findByReceiverIdInAndTypeAndArchivedTrue(receiverIds, type);

        List<InboxData> result = new ArrayList<>();
        for (Inbox inbox : inboxes) {
            InboxData dto = new InboxData();
            dto.setId(inbox.getId());
            dto.setSenderId(inbox.getSenderId());
            dto.setReceiverId(inbox.getReceiverId());
            String friendly = buildFriendlyContent(inbox);
            dto.setContent(friendly != null ? friendly : inbox.getContent());
            dto.setType(inbox.getType());
            dto.setEntityId(inbox.getEntityId());
            dto.setCreatedAt(inbox.getCreatedAt());
            dto.setRead(inbox.isRead());
            dto.setArchived(inbox.isArchived());
            result.add(dto);
        }
        return result;
    }

    // Compose human-friendly content for an inbox item
    private String buildFriendlyContent(Inbox inbox) {
        try {
            String type = inbox.getType();
            String senderEmail = inbox.getSenderId();
            String entityId = inbox.getEntityId();

            String senderName = null;
            try {
                User sender = userService.getUser(senderEmail);
                if (sender != null && sender.getFullName() != null && !sender.getFullName().isBlank()) {
                    senderName = sender.getFullName();
                }
            } catch (Exception ignored) {}
            String senderDisplay = senderName != null ? senderName : senderEmail;

            if ("PROJECT_INVITE".equalsIgnoreCase(type)) {
                if (entityId != null) {
                    Project p = null;
                    try { p = projectService.getProject(entityId); } catch (Exception ignored) {}
                    if (p != null) {
                        String workspace = p.getWorkspace() != null ? (" in workspace '" + p.getWorkspace() + "'") : "";
                        return String.format("%s invited you to join project '%s'%s.", senderDisplay, p.getProjectName(), workspace);
                    }
                }
                return String.format("%s invited you to a project.", senderDisplay);
            }

            if ("TASK_ASSIGNED".equalsIgnoreCase(type)) {
                if (entityId != null) {
                    Task t = null;
                    try { t = taskService.getTask(entityId); } catch (Exception ignored) {}
                    if (t != null) {
                        String projectPart = "";
                        try {
                            if (t.getProjectIds() != null && !t.getProjectIds().isEmpty()) {
                                Project p = projectService.getProject(t.getProjectIds().get(0));
                                if (p != null && p.getProjectName() != null) {
                                    projectPart = String.format(" in project '%s'", p.getProjectName());
                                }
                            }
                        } catch (Exception ignored) {}
                        return String.format("%s assigned you task '%s'%s.", senderDisplay, t.getTaskName(), projectPart);
                    }
                }
                return String.format("%s assigned you a task.", senderDisplay);
            }

            if ("PROJECT_MESSAGE".equalsIgnoreCase(type)) {
                if (entityId != null) {
                    Optional<Message> msgOpt = Optional.empty();
                    try { msgOpt = messageRepo.findById(entityId); } catch (Exception ignored) {}
                    if (msgOpt.isPresent()) {
                        Message m = msgOpt.get();
                        String subject = m.getSubject() != null && !m.getSubject().isBlank() ? m.getSubject() : "Message";
                        String snippet = m.getBody() != null ? (m.getBody().length() > 80 ? m.getBody().substring(0, 80) + "…" : m.getBody()) : "";
                        String projectPart = "";
                        try {
                            if (m.getProjectId() != null) {
                                Project p = projectService.getProject(m.getProjectId());
                                if (p != null && p.getProjectName() != null) {
                                    projectPart = String.format(" in project '%s'", p.getProjectName());
                                }
                            }
                        } catch (Exception ignored) {}
                        return String.format("%s posted a %s%s: %s", senderDisplay, subject, projectPart, snippet);
                    }
                }
                return String.format("%s sent you a message.", senderDisplay);
            }

            if ("GOAL_INVITE".equalsIgnoreCase(type)) {
                // We may not have easy access to goal title here; use generic phrasing
                return String.format("%s added you to a goal.", senderDisplay);
            }

            if ("PEOPLE_INVITE".equalsIgnoreCase(type)) {
                return String.format("%s invited you to collaborate.", senderDisplay);
            }

            // Self-confirmation types: content already human-friendly
            if ("FILE_UPLOAD_SELF".equalsIgnoreCase(type)
                    || "PROJECT_SELF_ADD".equalsIgnoreCase(type)
                    || "GOAL_SELF_ADD".equalsIgnoreCase(type)
                    || "TASK_SELF_ADD".equalsIgnoreCase(type)) {
                return inbox.getContent();
            }

            // Default fallback to stored content
            return inbox.getContent();
        } catch (Exception e) {
            // On any error, fallback to stored content
            return inbox.getContent();
        }
    }
}
