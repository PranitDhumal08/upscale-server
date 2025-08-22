package com.upscale.upscale.entity.project;

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
    private String id; // MongoDB will auto-generate ObjectId as String
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

    private List<String> subTaskIds = new ArrayList<>();

    // Recurrence configuration
    // NONE, WEEKLY, MONTHLY
    private String repeatFrequency;
    // For WEEKLY: 0=Sun..6=Sat
    private List<Integer> repeatDaysOfWeek = new ArrayList<>();
    // For MONTHLY: ON_NTH_WEEKDAY or ON_DAY_OF_MONTH
    private String monthlyMode;
    // For MONTHLY ON_NTH_WEEKDAY: 1..5 (1st, 2nd ... 5th) and weekday 0..6
    private Integer monthlyNth;
    private Integer monthlyWeekday;
    // For MONTHLY ON_DAY_OF_MONTH: 1..31
    private Integer monthlyDayOfMonth;

    // Periodic scheduling: create next task N days after completion
    private Integer periodicDaysAfterCompletion;

    // Recurrence linkage
    private String recurrenceParentId; // if this is an instance, points to parent template task
    private boolean recurrenceInstance; // true if generated instance
}
