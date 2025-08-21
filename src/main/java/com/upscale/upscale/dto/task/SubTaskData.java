package com.upscale.upscale.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SubTaskData {
    private String id;

    private String parentTaskId;

    private String taskName;
    private String sectionId;

        // For API bodies that pass a single project ID via path
    private String projectId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;

    private String priority;
    private String status;
    private boolean isCompleted;
    private String description;
    private List<String> assignId = new ArrayList<>();
    private List<String> projectIds = new ArrayList<>();
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

}
