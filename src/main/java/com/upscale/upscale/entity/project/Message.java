package com.upscale.upscale.entity.project;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "message")
public class Message {
    @Id
    private String id;
    private String subject;
    private String body;
    private String sender;
    private List<String> recipients = new ArrayList<>();
    private String projectId; // Project context for the message
}
