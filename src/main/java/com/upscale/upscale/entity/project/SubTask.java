package com.upscale.upscale.entity.project;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "subtasks")
public class SubTask {

    @Id
    private String id; // MongoDB will auto-generate ObjectId as String
    private String createdId;
    private String projectIds;
    private String taskName;
    private boolean isCompleted;
    private String priority;
    private String status;
    private String group;
    private Date date;
    private String description;
    private List<String> assignId = new ArrayList<>();
    private Date startDate;
    private Date endDate;

}
