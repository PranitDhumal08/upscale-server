package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;
    private String createdId;
    private List<String> projectIds = new ArrayList<>();
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
