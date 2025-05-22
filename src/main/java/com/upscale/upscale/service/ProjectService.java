package com.upscale.upscale.service;

import com.upscale.upscale.dto.ProjectCreate;
import com.upscale.upscale.entity.Project;
import com.upscale.upscale.repository.ProjectRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    private UserService userService;

    public void save(Project project){
        projectRepo.save(project);
    }
    public Project getProject(String emailId){
        return projectRepo.findByUserEmailid(emailId);
    }
    public boolean setProject(ProjectCreate projectCreate){

        if(getProject(projectCreate.getUserEmailid()) != null) return false;

        Project newProject = new Project();
        newProject.setUserEmailid(projectCreate.getUserEmailid());
        newProject.setProjectName(projectCreate.getProjectName());
        newProject.setWorkspace(projectCreate.getWorkspace());
        newProject.setRecommended(projectCreate.getRecommended());
        newProject.setPopular(projectCreate.getPopular());
        newProject.setOther(projectCreate.getOther());

        save(newProject);
        return userService.setProject(newProject, projectCreate.getUserEmailid());

    }
}
