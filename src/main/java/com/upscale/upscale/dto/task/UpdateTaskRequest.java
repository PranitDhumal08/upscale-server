package com.upscale.upscale.dto.task;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UpdateTaskRequest {
    // List of assignee email IDs; will be resolved to user IDs
    private List<String> assign;
    // Due date; mapped to Task.endDate
    private Date dueDate;
    private String priority;
    private String status;
}
