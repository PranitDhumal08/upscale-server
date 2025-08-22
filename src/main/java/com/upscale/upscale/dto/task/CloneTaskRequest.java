package com.upscale.upscale.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class CloneTaskRequest {
    private String id; // source task id (optional)
    private String taskName;
    private String createdId;
    private String description;
    private List<String> projectIds = new ArrayList<>();
    private Boolean completed; // if provided; default false on clone
    private String priority;
    private String status;
    private String group;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    // Assignees provided as emails; will be resolved to user IDs
    private List<String> assignId = new ArrayList<>();

    // Subtask IDs to clone under the new task
    private List<String> subTasks = new ArrayList<>();
}
