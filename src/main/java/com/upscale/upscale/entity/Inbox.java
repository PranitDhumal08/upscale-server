package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "inbox")
public class Inbox {

    @Id
    private String Id;
    private String senderId;
    private String receiverId;
    private String content;
}
