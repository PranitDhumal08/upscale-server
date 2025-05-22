package com.upscale.upscale.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectCreate {
        private String Id;
        private String userEmailid;
        private String projectName;
        private String workspace;
        private List<String> recommended; //overview,List,Board,timeline,dashboard
        private List<String> popular; //gantt,calender,note,workload
        private List<String> other; //file,message,workflow

}
