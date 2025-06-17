package com.upscale.upscale.service;

import com.upscale.upscale.dto.ProjectCreate;
import com.upscale.upscale.dto.ProjectData;
import com.upscale.upscale.dto.TaskData;
import com.upscale.upscale.entity.Project;
import com.upscale.upscale.entity.Task;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.repository.ProjectRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProjectService {

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private InboxService inboxService;

    public void save(Project project){
        projectRepo.save(project);
    }
    public Project getProject(String projectId){
        return projectRepo.findById(projectId).orElse(null);
    }

    public Task setTask(TaskData taskData, String createdId, String email) {

        log.info("Received TaskData: {}", taskData);

        Task task = new Task();
        task.setTaskName(taskData.getTaskName());
        task.setDate(taskData.getDate());
        task.setCompleted(false);
        task.setCreatedId(createdId);
        task.setProjectIds(taskData.getProjectIds());
        task.setDescription(taskData.getDescription());


        List<String> assignId = new ArrayList<>();
        for(String id:taskData.getAssignId()){

            if(id != createdId){
                inboxService.sendTaskDetails(task,email,id);
            }

            User user = userService.getUser(id);

            assignId.add(user.getId());
        }

        task.setAssignId(assignId);

        Task savedTask = taskService.save(task);
        log.info("Saved Task to DB: {}", savedTask);

        return savedTask;

    }

    public HashMap<String, List<Task>> getTasks(HashMap<String,List<String>> tasks){
        HashMap<String, List<Task>> newTasks = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : tasks.entrySet()) {
            String groupName = entry.getKey();
            List<String> taskNames = entry.getValue();

            List<Task> taskList = new ArrayList<>();
            for (String taskName : taskNames) {
                Task task = new Task();
                task.setTaskName(taskName);
                task.setCompleted(false);
                task.setDate(new Date());
                // Save the task to get an ID
                Task savedTask = taskService.save(task);
                taskList.add(savedTask);
            }

            newTasks.put(groupName, taskList);
        }

        return newTasks;
    }
    public boolean setProject(String emailId, ProjectCreate projectCreate) {
        if(emailId.isEmpty()) return false;

        Project newProject = new Project();

        newProject.setUserEmailid(emailId);
        newProject.setProjectName(projectCreate.getProjectName());
        newProject.setWorkspace(projectCreate.getWorkspace());
        newProject.setTasks(getTasks(projectCreate.getTasks()));
        newProject.setLayouts(projectCreate.getLayouts());
        newProject.setRecommended(projectCreate.getRecommended());
        newProject.setPopular(projectCreate.getPopular());
        newProject.setOther(projectCreate.getOther());
        
        // Process teammates and send invitations
        List<String> validTeammates = new ArrayList<>();
        for (String teammate : projectCreate.getTeammates()) {
            // Check if teammate exists in database
            User teammateUser = userService.getUser(teammate);
            if (teammateUser != null) {
                validTeammates.add(teammate);
                // Send project invitation via inbox
                inboxService.sendProjectInvite(emailId, teammate, newProject);
            } else {
                log.warn("Teammate not found in database: {}", teammate);
            }
        }
        newProject.setTeammates(validTeammates);

        save(newProject);
        return userService.setProject(newProject, emailId);
    }


    public boolean updateProject(String emailId, ProjectCreate projectCreate){
        Project project = getProject(emailId);

        if(project != null){

            if (project.getTasks().isEmpty()) {
                // Save or set the new tasks
                project.setTasks(getTasks(projectCreate.getTasks()));
            }
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

    public String getProjectName(String projectId){
        return projectRepo.findById(projectId).get().getProjectName();
    }

    public HashMap<String, String> getProjectsAsTeammate(String emailId) {
        HashMap<String, String> teammateProjects = new HashMap<>();
        List<Project> allProjects = projectRepo.findAll();
        
        for (Project project : allProjects) {
            if (project.getTeammates() != null && project.getTeammates().contains(emailId)) {
                teammateProjects.put(project.getId(), project.getProjectName());
            }
        }
        
        return teammateProjects;
    }
}
