package com.upscale.upscale.dto.project;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class ProjectCreate {

        private String Id; //project id
        //private String userEmailid; //main person mail id
        private String projectName; //project name
        private String workspace; //workspace name
        private HashMap<String,List<String>> tasks; //task in grops: task
        private String layouts; //layout gantt,board,timeline
        private List<String> recommended; //overview,List,Board,timeline,dashboard
        private List<String> popular; //gantt,calender,note,workload
        private List<String> other; //file,message,workflow
        private List<String> teammates; //teammates (still List for input, converted to HashMap in service)

}
