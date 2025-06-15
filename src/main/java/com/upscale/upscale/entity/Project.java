package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
@Document(collection = "projects")
public class Project {

    @Id
    private String id; //project id
    private String userEmailid; //main person mail id
    private String projectName; //project name
    private String projectDescription; //description
    private String workspace; //workspace name
    private HashMap<String,List<String>> tasks = new HashMap<>(); //task in grops: task
    private String layouts; //layout gantt,board,timeline
    private List<String> recommended = new ArrayList<>(); //overview,List,Board,timeline,dashboard
    private List<String> popular = new ArrayList<>(); //gantt,calender,note,workload
    private List<String> other = new ArrayList<>(); //file,message,workflow
    private List<String> teammates = new ArrayList<>(); //teammates
}
