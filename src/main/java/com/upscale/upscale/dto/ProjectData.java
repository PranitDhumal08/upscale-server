package com.upscale.upscale.dto;

import com.upscale.upscale.entity.Task;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class ProjectData {
    private String Id; //project id
    private String userEmailid; //main person mail id
    private String projectName; //project name
    private String workspace; //workspace name
    private HashMap<String, List<Task>> tasks = new HashMap<>(); //task in grops: task
    private String layouts; //layout gantt,board,timeline
    private List<String> recommended = new ArrayList<>(); //overview,List,Board,timeline,dashboard
    private List<String> popular = new ArrayList<>(); //gantt,calender,note,workload
    private List<String> other = new ArrayList<>(); //file,message,workflow
    private List<String> teammates = new ArrayList<>(); //teammates
}
