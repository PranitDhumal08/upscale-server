package com.upscale.upscale.controller.project;

import com.upscale.upscale.dto.project.ProjectCreate;
import com.upscale.upscale.dto.project.ProjectOverview;
import com.upscale.upscale.dto.project.SectionData;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.service.project.ProjectService;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.project.TaskService;
import com.upscale.upscale.dto.project.AddTaskToProjectRequest;
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
import java.util.Date;

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
                for (String taskId : section.getTaskIds()) {
                    Task task = taskService.getTask(taskId);
                    if (task != null) {
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
//        try {
//            Project project = projectService.getProject(projectId);
//            if (project == null) {
//                return new ResponseEntity<>("Project not found", HttpStatus.NOT_FOUND);
//            }
//
//            // Map taskId -> sectionId
//            Map<String, String> taskToSectionMap = new HashMap<>();
//            for (Section section : project.getSection()) {
//                for (Task task : section.getTasks()) {
//                    taskToSectionMap.put(task.getId(), section.getId()); // <taskId, sectionId>
//                }
//            }
//
//            List<Task> allTasksForProject = taskService.getTasksByProjectId(projectId);
//            HashMap<String, List<Object>> board = new HashMap<>();
//
//            for (Task task : allTasksForProject) {
//                String group = task.getGroup();
//                if (group == null || group.trim().isEmpty()) {
//                    group = "To do";
//                }
//
//                // Build assignee names
//                List<String> assigneeNames = new ArrayList<>();
//                for (String userId : task.getAssignId()) {
//                    String name = null;
//                    try {
//                        com.upscale.upscale.entity.user.User user = userService.getUserById(userId);
//                        if (user != null) name = user.getFullName();
//                    } catch (Exception e) {
//                        log.warn("Could not find user for id: {}", userId);
//                    }
//                    assigneeNames.add((name != null && !name.isEmpty()) ? name : userId);
//                }
//
//                // Build board card
//                HashMap<String, Object> card = new HashMap<>();
//                card.put("id", task.getId());
//                card.put("taskName", task.getTaskName());
//                card.put("priority", task.getPriority());
//                card.put("status", task.getStatus());
//                card.put("assignees", assigneeNames);
//                card.put("date", task.getDate());
//                card.put("description", task.getDescription());
//                card.put("completed", task.isCompleted());
//                card.put("sectionId", taskToSectionMap.get(task.getId())); // ✅ Add sectionId
//
//                board.computeIfAbsent(group, k -> new ArrayList<>()).add(card);
//            }
//
//            HashMap<String, Object> response = new HashMap<>();
//            response.put("message", ">>> Project board fetched successfully <<<");
//            response.put("board", board);
//            return new ResponseEntity<>(response, HttpStatus.OK);
//        } catch (Exception e) {
//            log.error("Error fetching project board for project ID: " + projectId, e);
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
        return getProjectTasks(projectId);
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

    @DeleteMapping("/delete/{project-id}")
    public ResponseEntity<?> deleteProject(HttpServletRequest request, @PathVariable("project-id") String projectId) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if(projectService.deleteProject(projectId)) {
                response.put("message", "Project deleted successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);

            }else{
                response.put("message", "Failed to delete project");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }catch (Exception e) {
            log.error("Error fetching project data: {}", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/section/{section-id}")
    public ResponseEntity<?> deleteSection(HttpServletRequest request, @PathVariable("section-id") String sectionId) {

        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if(projectService.deleteSection(sectionId)){
                response.put("message", "Section deleted successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                log.info("Section not found");
                response.put("message", "Section not found");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }catch (Exception e) {
            log.error("Error fetching project section data: {}", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{projectId}/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(@PathVariable("projectId") String projectId) {
        Map<String, Object> stats = projectService.getDashboardStats(projectId);
        // Map assignee IDs to names for upcomingTasksByAssignee
        if (stats.containsKey("upcomingTasksByAssignee")) {
            Map<String, Integer> byAssignee = (Map<String, Integer>) stats.get("upcomingTasksByAssignee");
            Map<String, Integer> mapped = new HashMap<>();
            for (Map.Entry<String, Integer> entry : byAssignee.entrySet()) {
                String name = entry.getKey();
                try {
                    com.upscale.upscale.entity.user.User user = userService.getUserById(entry.getKey());
                    if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                        name = user.getFullName();
                    }
                } catch (Exception ignored) {}
                mapped.put(name, entry.getValue());
            }
            stats.put("upcomingTasksByAssignee", mapped);
        }
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{projectId}/calendar-tasks")
    public ResponseEntity<?> getProjectCalendarTasks(
            @PathVariable("projectId") String projectId,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end
    ) {
        List<Task> tasks = taskService.getTasksByProjectId(projectId);
        Map<String, List<Map<String, Object>>> calendar = new HashMap<>();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Date startDate = null, endDate = null;
        try {
            if (start != null) startDate = fmt.parse(start);
            if (end != null) endDate = fmt.parse(end);
        } catch (Exception ignored) {}
        for (Task task : tasks) {
            Date taskStart = task.getStartDate() != null ? task.getStartDate() : task.getDate();
            if (taskStart == null) continue;
            // Filter by range if provided
            if (startDate != null && taskStart.before(startDate)) continue;
            if (endDate != null && taskStart.after(endDate)) continue;
            String dateKey = fmt.format(taskStart);
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("id", task.getId());
            taskInfo.put("title", task.getTaskName());
            // Assignees as names
            List<String> assigneeNames = new ArrayList<>();
            if (task.getAssignId() != null) {
                for (String userId : task.getAssignId()) {
                    String name = userId;
                    try {
                        User user = userService.getUserById(userId);
                        if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                            name = user.getFullName();
                        }
                    } catch (Exception ignored) {}
                    assigneeNames.add(name);
                }
            }
            taskInfo.put("assignees", assigneeNames);
            taskInfo.put("start", task.getStartDate() != null ? task.getStartDate() : task.getDate());
            taskInfo.put("end", task.getEndDate());
            taskInfo.put("status", task.isCompleted() ? "completed" : "incomplete");
            calendar.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(taskInfo);
        }
        return ResponseEntity.ok(calendar);
    }

    @GetMapping("/{projectId}/timeline")
    public ResponseEntity<?> getProjectTimeline(@PathVariable("projectId") String projectId) {
        Project project = projectService.getProject(projectId);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Project not found");
        }
        List<Map<String, Object>> timeline = new ArrayList<>();
        List<Section> sections = project.getSection();
        if (sections != null) {
            for (Section section : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("sectionName", section.getSectionName() != null ? section.getSectionName() : "Untitled section");
                sectionMap.put("sectionId", section.getId());
                List<Map<String, Object>> tasksList = new ArrayList<>();
                if (section.getTaskIds() != null) {
                    for (String taskId : section.getTaskIds()) {
                        Task task = taskService.getTask(taskId);
                        if (task != null) {
                            Map<String, Object> taskInfo = new HashMap<>();
                            taskInfo.put("id", task.getId());
                            taskInfo.put("taskName", task.getTaskName());
                            // Assignees as names/initials
                            List<String> assigneeNames = new ArrayList<>();
                            if (task.getAssignId() != null) {
                                for (String userId : task.getAssignId()) {
                                    String name = userId;
                                    try {
                                        User user = userService.getUserById(userId);
                                        if (user != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                                            name = user.getFullName();
                                        }
                                    } catch (Exception ignored) {}
                                    assigneeNames.add(name);
                                }
                            }
                            taskInfo.put("assignees", assigneeNames);
                            taskInfo.put("start", task.getStartDate() != null ? task.getStartDate() : task.getDate());
                            taskInfo.put("end", task.getEndDate());
                            taskInfo.put("status", task.isCompleted() ? "completed" : "incomplete");
                            tasksList.add(taskInfo);
                        }
                    }
                }
                sectionMap.put("tasks", tasksList);
                timeline.add(sectionMap);
            }
        }
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/overview-add/{project-id}")
    public ResponseEntity<?> overviewAdd(HttpServletRequest request, @PathVariable("project-id") String projectId, @RequestBody ProjectOverview projectOverview) {

        try {

            String email = tokenService.getEmailFromToken(request);

            HashMap<String,Object> response = new HashMap<>();

            if(projectService.updateProject(projectOverview,projectId)){
                response.put("status", "success");
                response.put("message", "Project updated successfully");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status", "failed");
                response.put("message", "Project update failed");
                return ResponseEntity.ok(response);
            }
        }catch (Exception e){
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get-overview/{project-id}")
    public ResponseEntity<?> getProjectOverview(HttpServletRequest request, @PathVariable("project-id") String projectId) {

        try {

            HashMap<String,Object> response = new HashMap<>();

            HashMap<String, Object> data = projectService.getProjectOverview(projectId);

            if(data != null){
                response.put("status", "success");
                response.put("message", "Project overview successfully");
                response.put("data", data);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else {
                response.put("status", "failed");
                response.put("message", "Project overview failed");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

            }

        }catch (Exception e){
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
