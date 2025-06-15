package com.upscale.upscale.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class TaskData {
    String taskName;
    Date date;
    private String description;
    private List<String> assignId = new ArrayList<>();
    private List<String> projectIds = new ArrayList<>();
}
