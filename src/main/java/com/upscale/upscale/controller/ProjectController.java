package com.upscale.upscale.controller;

import com.upscale.upscale.dto.ProjectCreate;
import com.upscale.upscale.dto.ProjectData;
import com.upscale.upscale.dto.SectionData;
import com.upscale.upscale.entity.Project;
import com.upscale.upscale.entity.Section;
import com.upscale.upscale.entity.Task;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.service.ProjectService;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.TaskService;
import com.upscale.upscale.dto.AddTaskToProjectRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserService userService;
    @Autowired
    private TaskService taskService;

    @PostMapping("/create-project")
    public ResponseEntity<?> createProject(HttpServletRequest request, @RequestBody ProjectCreate projectCreate) {
        try {
            HashMap<String, Object> response = new HashMap<>();
            String email = tokenService.getEmailFromToken(request);

            if(projectCreate != null){

                if(projectService.getProject(email) != null){
                    if(projectService.updateProject(email,projectCreate)){
                        response.put("message",">>> Project updated successfully <<<");
                        log.info("Project Updated: " + email + " successfully");
                        response.put("Data",projectCreate);
                    }
                    response.put("message", "Project already exists");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                else{

                    if(projectService.setProject(tokenService.getEmailFromToken(request),projectCreate)){
                        response.put("message",">>> Project created successfully <<<");
                        log.info("Project Created: " + email + " successfully");
                        response.put("Data",projectCreate);

                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }
                    else{
                        log.error("Failed to create project: " + email + "");
                        response.put("message",">>> Failed to create project <<<");

                        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }

            }else{
                response.put("message",">>> Invalid project data <<<");
                log.error("Invalid project data: " + email + "");

                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/dashboard/{project-id}")
    public ResponseEntity<?> getDashboard(HttpServletRequest request, @PathVariable("project-id") String projectId) {
        try{
            String email = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();
            if(projectService.getProject(projectId) != null){
                Project projectData = projectService.getProject(projectId);
                
                // Convert teammate email IDs to names
                List<String> teammateNames = new ArrayList<>();
                for(String teammateEmail : projectData.getTeammates()) {
                    String teammateName = userService.getName(teammateEmail);
                    if(teammateName != null && !teammateName.isEmpty()) {
                        teammateNames.add(teammateName);
                    }
                }
                projectData.setTeammates(teammateNames);

                response.put("Data", projectData);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            response.put("message", "Project not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list/{project-id}")
    public ResponseEntity<?> getProjectTasks(@PathVariable("project-id") String projectId) {
        try {
            Project project = projectService.getProject(projectId);
            if (project == null) {
                return new ResponseEntity<>("Project not found", HttpStatus.NOT_FOUND);
            }

            List<Section> sections = project.getSection();
            if (sections == null || sections.isEmpty()) {
                return new ResponseEntity<>("No sections or tasks found for this project", HttpStatus.OK);
            }

            List<Map<String, Object>> sectionList = new ArrayList<>();

            for (Section section : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("sectionId", section.getId());
                sectionMap.put("sectionName", section.getSectionName());

                List<Task> enrichedTasks = new ArrayList<>();
                for (Task task : section.getTasks()) {
                    List<String> nameList = new ArrayList<>();
                    for (String userId : task.getAssignId()) {
                        try {
                            User user = userService.getUserById(userId);
                            nameList.add((user != null && user.getFullName() != null) ? user.getFullName() : userId);
                        } catch (Exception e) {
                            log.warn("Could not find user for id: {}", userId);
                            nameList.add(userId);
                        }
                    }
                    task.setAssignId(nameList);
                    enrichedTasks.add(task);
                }

                sectionMap.put("tasks", enrichedTasks);
                sectionList.add(sectionMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", ">>> Project tasks fetched successfully <<<");
            response.put("tasks", sectionList);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching project tasks for project ID: {}", projectId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/{projectId}/tasks")
    public ResponseEntity<?> addTaskToProject(
            HttpServletRequest request,
            @PathVariable("projectId") String projectId,
            @RequestBody AddTaskToProjectRequest addTaskRequest) {
        try {
            String email = tokenService.getEmailFromToken(request);
            Task newTask = projectService.addTaskToProject(projectId, email, addTaskRequest);
            if (newTask != null) {
                return new ResponseEntity<>(newTask, HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>("Failed to add task or project not found", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error("Error adding task to project: {}", projectId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/board/{projectId}")
    public ResponseEntity<?> getProjectBoard(@PathVariable("projectId") String projectId) {
        try {
            if (projectService.getProject(projectId) == null) {
                return new ResponseEntity<>("Project not found", HttpStatus.NOT_FOUND);
            }

            List<Task> allTasksForProject = taskService.getTasksByProjectId(projectId);
            HashMap<String, List<Object>> board = new HashMap<>();
            for (Task task : allTasksForProject) {
                String group = task.getGroup();
                if (group == null || group.trim().isEmpty()) {
                    group = "To do";
                }
                // Build a board card object for the frontend
                List<String> assigneeNames = new ArrayList<>();
                for (String userId : task.getAssignId()) {
                    String name = null;
                    try {
                        com.upscale.upscale.entity.User user = userService.getUserById(userId);
                        if (user != null) name = user.getFullName();
                    } catch (Exception e) {
                        log.warn("Could not find user for id: {}", userId);
                    }
                    if (name != null && !name.isEmpty()) {
                        assigneeNames.add(name);
                    } else {
                        assigneeNames.add(userId);
                    }
                }
                HashMap<String, Object> card = new HashMap<>();
                card.put("id", task.getId());
                card.put("taskName", task.getTaskName());
                card.put("priority", task.getPriority());
                card.put("status", task.getStatus());
                card.put("assignees", assigneeNames);
                card.put("date", task.getDate());
                card.put("description", task.getDescription());
                card.put("completed", task.isCompleted());
                // Add more fields as needed for your board UI
                board.computeIfAbsent(group, k -> new ArrayList<>()).add(card);
            }
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", ">>> Project board fetched successfully <<<");
            response.put("board", board);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching project board for project ID: " + projectId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/add-section/{project-id}")
    public ResponseEntity<?> addSectionToProject(HttpServletRequest request, @RequestBody SectionData sectionData, @PathVariable("project-id") String projectId) {

        try {

            String email = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();

            if(sectionData != null) {

                if(projectService.addProjectSection(projectId,sectionData)) {
                    log.info("Added section to project: {}", projectId);
                    response.put("message", "Added section to project");

                    return new ResponseEntity<>(response, HttpStatus.CREATED);
                }
                else{
                    response.put("message", "Failed to add section to project");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }else{
                log.error("Project not found");
                response.put("message", "Project not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        }catch (Exception e) {
            log.error("Error fetching project section data: {}", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
