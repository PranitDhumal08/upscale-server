package com.upscale.upscale.dto.project;

import lombok.Data;
import java.util.Date;

@Data
public class InboxData {

    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private String type;
    private String entityId;
    private Date createdAt;
    private boolean read;
}
