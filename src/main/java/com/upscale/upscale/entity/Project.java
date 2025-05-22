package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "projects")
public class Project {
    private String Id;
    private String userEmailid;
    private String projectName;
    private String workspace;
    private List<String> recommended; //overview,List,Board,timeline,dashboard
    private List<String> popular; //gantt,calender,note,workload
    private List<String> other; //file,message,workflow
}
