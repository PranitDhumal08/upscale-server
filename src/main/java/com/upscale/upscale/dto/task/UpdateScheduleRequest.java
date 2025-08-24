package com.upscale.upscale.dto.task;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UpdateScheduleRequest {
    private Date startDate;   // required
    private Date endDate;     // required

    // Recurrence
    // NONE, DAILY, WEEKLY, MONTHLY, PERIODICALLY
    private String repeatFrequency; // optional, default NONE

    // Weekly options: 0=Sun..6=Sat
    private List<Integer> daysOfWeek; // optional when WEEKLY

    // Monthly options
    // ON_NTH_WEEKDAY or ON_DAY_OF_MONTH
    private String monthlyMode; // optional when MONTHLY
    private Integer monthlyNth; // 1..5
    private Integer monthlyWeekday; // 0..6
    private Integer monthlyDayOfMonth; // 1..31

    // Periodically options
    // Number of days after completion to schedule next instance
    private Integer periodicDaysAfterCompletion; // optional when PERIODICALLY

    // Clone subtasks for each instance (default true)
    private Boolean cloneSubTasks;
}
