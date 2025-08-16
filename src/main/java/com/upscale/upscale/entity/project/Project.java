package com.upscale.upscale.entity.project;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
@Document(collection = "projects")
public class Project {

    @Id
    private String id; //project id
    
    @Field("userEmailid")
    private String userEmailid; //main person mail id
    private String projectName; //project name
    private String projectDescription; //description
    private String workspace; //workspace name
    //private HashMap<String, List<String>> tasks = new HashMap<>(); // task groups: task IDs
    private String layouts; //layout gantt,board,timeline
    private List<String> recommended = new ArrayList<>(); //overview,List,Board,timeline,dashboard
    private List<String> popular = new ArrayList<>(); //gantt,calender,note,workload
    private List<String> other = new ArrayList<>(); //file,message,workflow
    private HashMap<String,String[]> teammates = new HashMap<>(); //teammates
//    Key: User's name
//    Value: String array with 4 elements:
//           [0]: emailid
//           [1]: Role in the project
//           [2]: Position (owner/employee)
//           [3]: name
    private List<Section> section = new ArrayList<>(); // Sections in the project

    private Date startDate;
    private Date endDate;

    private String portfolioPriority;

    private String status;
}
