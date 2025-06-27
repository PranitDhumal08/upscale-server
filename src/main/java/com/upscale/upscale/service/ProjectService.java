package com.upscale.upscale.service;

import com.upscale.upscale.dto.*;
import com.upscale.upscale.entity.Project;
import com.upscale.upscale.entity.Section;
import com.upscale.upscale.entity.Task;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.repository.ProjectRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
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

    @Autowired
    private SectionService sectionService;

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

    public HashMap<String, List<String>> getTasks(String projectId, List<String> teammates, String creatorId, HashMap<String,List<String>> tasks){
        HashMap<String, List<String>> newTasks = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : tasks.entrySet()) {
            String groupName = entry.getKey();
            List<String> taskNames = entry.getValue();

            List<String> taskIds = new ArrayList<>();
            for (String taskName : taskNames) {
                Task task = new Task();
                task.setTaskName(taskName);
                task.setGroup(groupName);
                task.setCompleted(false);
                task.setDate(new Date());
                task.setProjectIds(Collections.singletonList(projectId)); // Set project ID
                task.setAssignId(teammates); // Assign to all teammates
                task.setCreatedId(creatorId); // Set the creator ID
                // Save the task to get an ID
                Task savedTask = taskService.save(task);
                taskIds.add(savedTask.getId());
            }

            newTasks.put(groupName, taskIds);
        }

        return newTasks;
    }

    public List<Section> getSections(String projectId, List<String> teammates, String creatorId, HashMap<String, List<String>> tasksMap) {
        List<Section> sections = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : tasksMap.entrySet()) {
            String sectionName = entry.getKey();
            List<String> taskNames = entry.getValue();

            Section section = new Section();
            section.setSectionName(sectionName);
            List<Task> taskList = new ArrayList<>();

            for (String taskName : taskNames) {
                Task task = new Task();
                task.setTaskName(taskName);
                task.setGroup(sectionName);
                task.setCompleted(false);
                task.setDate(new Date());
                task.setProjectIds(Collections.singletonList(projectId));
                task.setAssignId(teammates);
                task.setCreatedId(creatorId);

                Task savedTask = taskService.save(task);
                taskList.add(savedTask);
            }

            section.setTasks(taskList);
            sectionService.save(section);
            sections.add(section);
        }

        return sections;
    }

    public boolean setProject(String emailId, ProjectCreate projectCreate) {
        if(emailId.isEmpty()) return false;

        Project newProject = new Project();

        newProject.setUserEmailid(emailId);
        newProject.setProjectName(projectCreate.getProjectName());
        newProject.setWorkspace(projectCreate.getWorkspace());

        // Save the project first to get an ID before setting tasks
        save(newProject);

        // Get the creator's ID
        String creatorId = userService.getUser(emailId).getId();

        // Now pass the project ID, teammates, and creator ID to getTasks
        List<Section> sections = getSections(newProject.getId(), projectCreate.getTeammates(), creatorId, projectCreate.getTasks());
        newProject.setSection(sections);

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



        save(newProject); // Save again with updated tasks and teammates
        return userService.setProject(newProject, emailId);
    }


    public boolean updateProject(String emailId, ProjectCreate projectCreate){
        Project project = getProject(emailId);

        if(project != null){

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
        
        // This method now needs to resolve task IDs to Task objects.
        // For now, I'll leave it empty as the main focus is the /list/{project-id} endpoint.
        // projectData.setTasks(...); 
        
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

    public Task addTaskToProject(String projectId, String creatorEmail, AddTaskToProjectRequest addTaskRequest) {
        Project project = getProject(projectId);
        if (project == null) {
            log.error("Project not found with id: {}", projectId);
            return null;
        }

        User creator = userService.getUser(creatorEmail);
        if (creator == null) {
            log.error("Creator user not found with email: {}", creatorEmail);
            return null;
        }

        Task task = new Task();
        task.setTaskName(addTaskRequest.getTaskName());
        task.setDescription(addTaskRequest.getDescription());
        task.setDate(addTaskRequest.getDate());
        task.setPriority(addTaskRequest.getPriority());
        task.setStatus(addTaskRequest.getStatus());
        task.setCompleted(false);
        task.setCreatedId(creator.getId());
        String group = addTaskRequest.getGroup();
        if (group == null || group.trim().isEmpty()) {
            group = "To do"; // Default group
        }
        task.setGroup(group);
        task.setProjectIds(Collections.singletonList(projectId));

        List<String> assignIds = new ArrayList<>();
        if (addTaskRequest.getAssignId() != null) {
            for (String assigneeEmail : addTaskRequest.getAssignId()) {
                User assignee = userService.getUser(assigneeEmail);
                if (assignee != null) {
                    assignIds.add(assignee.getId());
                    if (!assignee.getId().equals(creator.getId())) {
                        inboxService.sendTaskDetails(task, creatorEmail, assigneeEmail);
                    }
                } else {
                    log.warn("Assignee user not found for email: {}", assigneeEmail);
                }
            }
        }
        task.setAssignId(assignIds);

        Task savedTask = taskService.save(task);
        log.info("Saved new task with id: {}", savedTask.getId());

        //project.getTasks().computeIfAbsent(group, k -> new ArrayList<>()).add(savedTask.getId());

        save(project);
        log.info("Updated project {} with new task {}", projectId, savedTask.getId());

        return savedTask;
    }

    public boolean addProjectSection(String projectId, SectionData sectionData) {
        if(sectionData == null) return false;

        Project project = getProject(projectId);
        if(project == null) return false;


        Section section = new Section();
        section.setSectionName(sectionData.getSectionName());

        sectionService.save(section);
        project.getSection().add(section);


        return true;
    }
}
