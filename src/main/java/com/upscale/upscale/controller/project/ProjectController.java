package com.upscale.upscale.controller.project;

import com.upscale.upscale.dto.project.ProjectCreate;
import com.upscale.upscale.dto.project.ProjectOverview;
import com.upscale.upscale.dto.project.SectionData;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.entity.project.SubTask;
import com.upscale.upscale.repository.SubTaskRepo;
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
    @Autowired
    private SubTaskRepo subTaskRepo;

    @PostMapping("/create-project")
    public ResponseEntity<?> createProject(HttpServletRequest request, @RequestBody ProjectCreate projectCreate) {
        try {
            HashMap<String, Object> response = new HashMap<>();
            String email = tokenService.getEmailFromToken(request);

            if(projectCreate != null){

                // Validate teammate emails before creating/updating project
                if (projectCreate.getTeammates() != null && !projectCreate.getTeammates().isEmpty()) {
                    for (String teammateEmail : projectCreate.getTeammates()) {
                        try {
                            User teammate = userService.getUser(teammateEmail);
                            if (teammate == null) {
                                response.put("message", "teammate user not valid");
                                response.put("invalidEmail", teammateEmail);
                                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                            }
                        } catch (Exception ex) {
                            response.put("message", "teammate user not valid");
                            response.put("invalidEmail", teammateEmail);
                            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                        }
                    }
                }

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

    @DeleteMapping("/overview/remove-member/{user-id}/project/{project-id}")
    public ResponseEntity<?> removeMemberFromProject(
            HttpServletRequest request,
            @PathVariable("user-id") String userId,
            @PathVariable("project-id") String projectId) {

        try {
            String requesterEmail = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            // Validate project
            Project project = projectService.getProject(projectId);
            if (project == null) {
                response.put("status", "error");
                response.put("message", "Project not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            HashMap<String, String[]> teammates = project.getTeammates();
            if (teammates == null || teammates.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No teammates found for this project");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Try to find target teammate entry by key (userId) first; if not found, fallback by email
            String removalKey = userId;
            String[] targetInfo = teammates.get(removalKey);
            String targetEmail = null;
            if (targetInfo == null || targetInfo.length < 3) {
                // Fallback: resolve user by ID to get email, then find by email in teammates values
                User targetUserResolved = null;
                try { targetUserResolved = userService.getUserById(userId); } catch (Exception ignored) {}
                if (targetUserResolved != null && targetUserResolved.getEmailId() != null) {
                    String emailToFind = targetUserResolved.getEmailId();
                    for (Map.Entry<String, String[]> entry : teammates.entrySet()) {
                        String[] info = entry.getValue();
                        if (info != null && info.length > 2 && emailToFind.equalsIgnoreCase(info[2])) {
                            removalKey = entry.getKey();
                            targetInfo = info;
                            break;
                        }
                    }
                }
            }
            if (targetInfo == null || targetInfo.length < 3) {
                response.put("status", "error");
                response.put("message", "User not found in project teammates");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            targetEmail = targetInfo[2];

            // Prevent removing project owner
            if (project.getUserEmailid() != null && project.getUserEmailid().equalsIgnoreCase(targetEmail)) {
                response.put("status", "error");
                response.put("message", "Cannot remove project owner");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // Permission: requester must be owner or admin teammate
            boolean isOwner = project.getUserEmailid() != null && project.getUserEmailid().equalsIgnoreCase(requesterEmail);
            boolean isAdmin = false;
            if (!isOwner) {
                for (Map.Entry<String, String[]> entry : teammates.entrySet()) {
                    String[] info = entry.getValue();
                    if (info != null && info.length > 2 && requesterEmail.equalsIgnoreCase(info[2])) {
                        if (info[1] != null && info[1].equalsIgnoreCase("admin")) {
                            isAdmin = true;
                        }
                        break;
                    }
                }
            }

            if (!(isOwner || isAdmin)) {
                response.put("status", "error");
                response.put("message", "You don't have permission to remove this member");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // Remove member from project teammates
            teammates.remove(removalKey);
            project.setTeammates(teammates);
            projectService.save(project);

            // Also remove project from user's project list if user exists
            try {
                User targetUser = userService.getUserById(userId);
                if (targetUser != null && targetUser.getProjects() != null) {
                    List<String> projList = targetUser.getProjects();
                    projList.removeIf(id -> id.equals(projectId));
                    targetUser.setProjects(projList);
                    userService.save(targetUser);
                }
            } catch (Exception ignored) {}

            response.put("status", "success");
            response.put("message", "Member removed from project successfully");
            response.put("projectId", projectId);
            response.put("removedUserId", userId);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error(e.getMessage());
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
                
                // ProjectData now uses HashMap<String,String[]> for teammates, so no conversion needed
                // The teammates HashMap is already in the correct format

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

                List<Map<String, Object>> enrichedTasks = new ArrayList<>();
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
                        // Build task map without subTaskIds; include resolved subtasks instead
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("id", task.getId());
                        taskMap.put("createdId", task.getCreatedId());
                        taskMap.put("projectIds", task.getProjectIds());
                        taskMap.put("taskName", task.getTaskName());
                        taskMap.put("priority", task.getPriority());
                        taskMap.put("status", task.getStatus());
                        taskMap.put("group", task.getGroup());
                        taskMap.put("date", task.getDate());
                        taskMap.put("description", task.getDescription());
                        taskMap.put("assignId", nameList);
                        taskMap.put("startDate", task.getStartDate());
                        taskMap.put("endDate", task.getEndDate());
                        taskMap.put("completed", task.isCompleted());

                        // Resolve subtasks and compute progressBar
                        List<Map<String, Object>> subTasks = new ArrayList<>();
                        int completedCount = 0;
                        int totalCount = 0;
                        if (task.getSubTaskIds() != null) {
                            for (String subTaskId : task.getSubTaskIds()) {
                                try {
                                    SubTask st = subTaskRepo.findById(subTaskId).orElse(null);
                                    if (st != null) {
                                        Map<String, Object> stMap = new HashMap<>();
                                        // Include subtask id as requested
                                        stMap.put("id", st.getId());
                                        stMap.put("createdId", st.getCreatedId());
                                        stMap.put("projectIds", st.getProjectIds());
                                        stMap.put("taskName", st.getTaskName());
                                        stMap.put("priority", st.getPriority());
                                        stMap.put("status", st.getStatus());
                                        stMap.put("group", st.getGroup());
                                        stMap.put("date", st.getDate());
                                        stMap.put("description", st.getDescription());
                                        // Map subtask assignee IDs to names (fallback to ID if name not available)
                                        List<String> subAssigneeNames = new ArrayList<>();
                                        if (st.getAssignId() != null) {
                                            for (String subUserId : st.getAssignId()) {
                                                String name = subUserId;
                                                try {
                                                    User su = userService.getUserById(subUserId);
                                                    if (su != null && su.getFullName() != null && !su.getFullName().isEmpty()) {
                                                        name = su.getFullName();
                                                    }
                                                } catch (Exception ignored) {}
                                                subAssigneeNames.add(name);
                                            }
                                        }
                                        stMap.put("assignId", subAssigneeNames);
                                        stMap.put("startDate", st.getStartDate());
                                        stMap.put("endDate", st.getEndDate());
                                        stMap.put("completed", st.isCompleted());
                                        subTasks.add(stMap);
                                        totalCount++;
                                        if (st.isCompleted()) completedCount++;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                        int progress;
                        if (totalCount > 0) {
                            // Progress is based on subtasks when they exist
                            progress = (int) Math.floor((completedCount * 100.0) / totalCount);
                            // If parent task marked complete but not all subtasks are complete, cap at 99
                            if (progress < 100 && task.isCompleted()) {
                                progress = 99;
                            }
                            // If all subtasks complete but parent not marked complete, cap at 99
                            if (progress == 100 && !task.isCompleted()) {
                                progress = 99;
                            }
                        } else {
                            // No subtasks: reflect parent completion state
                            progress = task.isCompleted() ? 100 : 0;
                        }
                        taskMap.put("progressBar", progress);
                        taskMap.put("subTasks", subTasks);

                        enrichedTasks.add(taskMap);
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

    @PostMapping("/overview/duplicate/{project-id}")
    public ResponseEntity<?> overviewDuplicate(HttpServletRequest request, @PathVariable("project-id") String projectId) {

        try {

            String email = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();
            User user = userService.getUser(email); // Fixed: use getUser instead of getUserById for email
            
            if(user != null){
                
                // Get the original project
                Project originalProject = projectService.getProject(projectId);
                if(originalProject == null) {
                    response.put("status", "error");
                    response.put("message", "Original project not found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
                
                // Check if user has access to the original project (owner or teammate)
                boolean hasAccess = originalProject.getUserEmailid().equals(email);
                
                // Check if user is in teammates HashMap
                if (!hasAccess && originalProject.getTeammates() != null) {
                    for (String[] teammateInfo : originalProject.getTeammates().values()) {
                        if (teammateInfo.length > 2 && teammateInfo[2].equals(email)) {
                            hasAccess = true;
                            break;
                        }
                    }
                }
                
                if(!hasAccess) {
                    response.put("status", "error");
                    response.put("message", "You don't have permission to duplicate this project");
                    return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
                }
                
                // Duplicate the project
                Project duplicatedProject = projectService.duplicateProject(originalProject, email);
                
                if(duplicatedProject != null) {
                    // Add the duplicated project to user's projects list
                    user.getProjects().add(duplicatedProject.getId());
                    userService.save(user);
                    
                    response.put("status", "success");
                    response.put("message", "Project duplicated successfully");
                    response.put("originalProjectId", projectId);
                    response.put("duplicatedProjectId", duplicatedProject.getId());
                    response.put("duplicatedProjectName", duplicatedProject.getProjectName());
                    
                    log.info("Project {} duplicated successfully by user {}", projectId, email);
                    return new ResponseEntity<>(response, HttpStatus.CREATED);
                } else {
                    response.put("status", "error");
                    response.put("message", "Failed to duplicate project");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
                
            } else {
                response.put("status", "error");
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        }catch (Exception e){
            log.error("Error duplicating project: ", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/overview/change-role/{user-id}/project/{project-id}/{role-name}")
    public ResponseEntity<?> changeRoleOfUser(HttpServletRequest request, @PathVariable("user-id") String userId, @PathVariable("project-id") String projectId, @PathVariable("role-name") String roleName) {

        try {

            String email = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            // Fetch project and validate
            Project originalProject = projectService.getProject(projectId);
            if (originalProject == null) {
                response.put("status", "error");
                response.put("message", "Project not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            HashMap<String,String[]> teammates = originalProject.getTeammates();
            if (teammates == null || teammates.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No teammates found for this project");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String[] teammateInfo = teammates.get(userId);
            if (teammateInfo == null || teammateInfo.length < 3) {
                response.put("status", "error");
                response.put("message", "User not found in project teammates");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // Permission: project owner OR an admin teammate can change roles
            boolean isOwner = originalProject.getUserEmailid() != null && originalProject.getUserEmailid().equalsIgnoreCase(email);
            boolean isAdmin = false;

            if (!isOwner) {
                // Find requester in teammates by matching email at index [2]
                for (Map.Entry<String, String[]> entry : teammates.entrySet()) {
                    String[] info = entry.getValue();
                    if (info != null && info.length > 2 && email.equalsIgnoreCase(info[2])) {
                        // role is at index [1]
                        if (info[1] != null && info[1].equalsIgnoreCase("admin")) {
                            isAdmin = true;
                        }
                        break;
                    }
                }
            }

            if (isOwner || isAdmin) {
                // Update role for the target teammate
                teammateInfo[1] = roleName;
                teammates.put(userId, teammateInfo);
                originalProject.setTeammates(teammates);
                projectService.save(originalProject);
                response.put("status", "success");
                response.put("message", "Project updated successfully");
                response.put("originalProjectId", projectId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("status", "error");
                response.put("message", "You don't have permission to change this project");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }


        }catch (Exception e){
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/overview/{project-id}/change-owner/{user-id}")
    public ResponseEntity<?> changeProjectOwner(HttpServletRequest request, @PathVariable("user-id") String userId, @PathVariable("project-id") String projectid) {

        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();
            
            // Check if project exists
            Project originalProject = projectService.getProject(projectid);
            if (originalProject == null) {
                response.put("status", "error");
                response.put("message", "Project not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            
            // Check if current user is the project owner
            if (!originalProject.getUserEmailid().equals(email)) {
                response.put("status", "error");
                response.put("message", "Only project owner can change ownership");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }
            
            HashMap<String,String[]> teammates = originalProject.getTeammates();
            if(teammates == null || teammates.isEmpty()){
                response.put("status", "error");
                response.put("message", "No teammates found in project");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // Find the target user in teammates
            String[] targetUserInfo = teammates.get(userId);
            if (targetUserInfo == null) {
                response.put("status", "error");
                response.put("message", "Target user not found in project teammates");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            
            // Find current owner in teammates and change to employee
            String currentOwnerId = null;
            for (Map.Entry<String, String[]> entry : teammates.entrySet()) {
                String[] memberInfo = entry.getValue();
                if (memberInfo.length >= 2 && "owner".equals(memberInfo[1])) {
                    memberInfo[1] = "employee"; // Change position from owner to employee
                    teammates.put(entry.getKey(), memberInfo);
                    currentOwnerId = entry.getKey();
                    break;
                }
            }
            
            // Change target user to owner
            if (targetUserInfo.length >= 2) {
                targetUserInfo[1] = "owner"; // Change position to owner
                teammates.put(userId, targetUserInfo);
            }
            
            // Get target user's email for updating project owner
            String newOwnerEmail = null;
            if (targetUserInfo.length >= 3) {
                // Handle different data formats
                if (targetUserInfo.length == 3) {
                    newOwnerEmail = targetUserInfo[2]; // Format: [role, position, email]
                } else if (targetUserInfo.length >= 4) {
                    newOwnerEmail = targetUserInfo[0]; // Format: [email, role, position, name]
                }
            }
            
            // Update project
            originalProject.setTeammates(teammates);
            if (newOwnerEmail != null) {
                originalProject.setUserEmailid(newOwnerEmail);
            }
            projectService.save(originalProject);
            
            response.put("status", "success");
            response.put("message", "Project ownership changed successfully");
            response.put("projectId", projectid);
            response.put("newOwnerId", userId);
            response.put("previousOwnerId", currentOwnerId);
            
            log.info("Project {} ownership changed from {} to {}", projectid, currentOwnerId, userId);
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        }catch (Exception e){
            log.error("Error changing project ownership: ", e);
            HashMap<String,Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Internal server error: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
