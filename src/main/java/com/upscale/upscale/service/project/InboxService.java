package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.InboxData;
import com.upscale.upscale.entity.Goal;
import com.upscale.upscale.entity.project.*;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.InboxRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@Service
@Slf4j
public class InboxService {

    @Autowired
    private InboxRepo inboxRepo;

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
        List<Inbox> inboxes = (type == null || type.isBlank())
                ? inboxRepo.findByReceiverId(emailId)
                : inboxRepo.findByReceiverIdAndType(emailId, type);

        List<InboxData> inboxDataList = new ArrayList<>();

        if(inboxes != null && !inboxes.isEmpty()){
            for (Inbox inbox : inboxes) {
                InboxData inboxData = new InboxData();

                inboxData.setId(inbox.getId());
                inboxData.setSenderId(inbox.getSenderId());
                inboxData.setReceiverId(inbox.getReceiverId());
                inboxData.setContent(inbox.getContent());
                inboxData.setType(inbox.getType());
                inboxData.setEntityId(inbox.getEntityId());
                inboxData.setCreatedAt(inbox.getCreatedAt());
                inboxData.setRead(inbox.isRead());

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
            if (!receiverEmail.equals(inbox.getReceiverId())) return false; // prevent others marking it
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
}
