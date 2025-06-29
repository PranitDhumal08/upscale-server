package com.upscale.upscale.dto.project;

import lombok.Data;

import java.util.List;

@Data
public class GoalData {
    private String userId;
    private String goalTitle;
    private String goalOwner;
    private String timePeriod;
    private String privacy;
    private List<String> members;
}
