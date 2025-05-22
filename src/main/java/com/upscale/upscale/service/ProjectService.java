package com.upscale.upscale.service;

import com.upscale.upscale.dto.ProjectCreate;
import com.upscale.upscale.dto.ProjectData;
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
    public boolean setProject(String emailId,ProjectCreate projectCreate){

        if(emailId.isEmpty()) return false;

        Project newProject = new Project();

        newProject.setUserEmailid(emailId);
        newProject.setProjectName(projectCreate.getProjectName());
        newProject.setWorkspace(projectCreate.getWorkspace());
        newProject.setTasks(projectCreate.getTasks());
        newProject.setLayouts(projectCreate.getLayouts());
        newProject.setRecommended(projectCreate.getRecommended());
        newProject.setPopular(projectCreate.getPopular());
        newProject.setOther(projectCreate.getOther());
        newProject.setTeammates(projectCreate.getTeammates());

        save(newProject);
        return userService.setProject(newProject, emailId);

    }

    public boolean updateProject(String emailId, ProjectCreate projectCreate){
        Project project = getProject(emailId);

        if(project != null){

            if(project.getTasks().isEmpty()) project.setTasks(projectCreate.getTasks());
            if(project.getLayouts().isEmpty()) project.setLayouts(projectCreate.getLayouts());
            if(project.getRecommended().isEmpty()) project.setRecommended(projectCreate.getRecommended());
            if(project.getPopular().isEmpty()) project.setPopular(projectCreate.getPopular());
            if(project.getOther().isEmpty()) project.setOther(projectCreate.getOther());
            if(project.getTeammates().isEmpty()) project.setTeammates(projectCreate.getTeammates());

            save(project);
            return true;
        }

        return false;
    }

    public ProjectData getInfo(String emailId){
        Project project = getProject(emailId);
        ProjectData projectData = new ProjectData();
        projectData.setProjectName(project.getProjectName());
        projectData.setWorkspace(project.getWorkspace());
        projectData.setTasks(project.getTasks());
        projectData.setLayouts(project.getLayouts());
        projectData.setRecommended(project.getRecommended());
        projectData.setPopular(project.getPopular());
        projectData.setOther(project.getOther());
        projectData.setTeammates(project.getTeammates());
        return projectData;
    }
}
