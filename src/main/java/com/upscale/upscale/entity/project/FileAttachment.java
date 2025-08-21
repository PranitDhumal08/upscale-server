package com.upscale.upscale.entity.project;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "file_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachment {
    @Id
    private String id;

    private String projectId;
    private String senderId; // stored as userId
    private List<String> receiverIds = new ArrayList<>(); // stored as userIds

    private String fileName; // stored name
    private String originalFileName; // uploaded name
    private String displayName; // user-defined name for displaying/downloading
    private String fileType; // PDF | EXCEL | PPT | IMAGE
    private String mimeType;
    private long fileSize;
    private byte[] fileData; // binary content

    private Date uploadDate;
    private String description;
}
