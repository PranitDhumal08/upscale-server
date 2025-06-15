package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;
    private String createdId;
    private String taskName;
    private boolean isCompleted;
    private Date date;
    private String description;
    private List<String> assignId;

}
