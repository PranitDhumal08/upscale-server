package com.upscale.upscale.dto.project;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GoalData {
    private String goalId;
    private String userId;
    private String goalTitle;
    private String goalOwner;
    private String timePeriod;
    private String privacy;
    private List<String> members;
    private List<String> projectIds;
    private Double completionPercentage; // Goal completion percentage based on project tasks
    private Integer totalTasks; // Total tasks in associated projects
    private Integer completedTasks; // Completed tasks in associated projects

    private String parentGoalId;

    private List<GoalData> children = new ArrayList<>();
}
