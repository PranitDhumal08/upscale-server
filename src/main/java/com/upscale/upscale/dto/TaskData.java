package com.upscale.upscale.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TaskData {
    private String id;
    private String taskName;
    private String sectionId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;
    
    private String priority;
    private String status;
    private boolean isCompleted;
    private String description;
    private List<String> assignId = new ArrayList<>();
    private List<String> projectIds = new ArrayList<>();
}
