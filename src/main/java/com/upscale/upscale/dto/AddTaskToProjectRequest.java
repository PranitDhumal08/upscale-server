package com.upscale.upscale.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AddTaskToProjectRequest {
    private String taskName;
    private String description;
    private List<String> assignId; // list of emails
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;
    private String priority;
    private String status;
    private String group; // e.g., "To do", "Doing", "Done"
} 