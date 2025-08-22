package com.upscale.upscale.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UpdateTaskRequest {
    // List of assignee email IDs; will be resolved to user IDs
    private List<String> assign;
    // Optional scheduling fields
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
    private String priority;
    private String status;
}
