package com.upscale.upscale.entity.project;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "inbox")
public class Inbox {

    @Id
    private String Id;
    private String senderId;
    private String receiverId;
    private String content;
    private String type;      // e.g. PROJECT_INVITE, TASK_ASSIGNED, GOAL_INVITE, PROJECT_MESSAGE, PEOPLE_INVITE
    private String entityId;  // related entity id (projectId, taskId, goalId) if available
    private Date createdAt;   // when the message was created
    private boolean read;     // has the receiver read this message
}
