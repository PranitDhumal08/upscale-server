package com.upscale.upscale.controller.project;


import com.upscale.upscale.dto.task.TaskData;
import com.upscale.upscale.dto.task.CloneTaskRequest;
import com.upscale.upscale.dto.task.UpdateScheduleRequest;
import com.upscale.upscale.dto.task.UpdateTaskRequest;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.project.SubTask;
import com.upscale.upscale.repository.SubTaskRepo;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.service.project.ProjectService;
import com.upscale.upscale.service.project.SubTaskService;
import com.upscale.upscale.service.project.TaskService;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/task")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SubTaskRepo subTaskRepo;


    @PostMapping("/set-task")
    public ResponseEntity<?> setTask(HttpServletRequest request, @RequestBody TaskData taskData) {
        try {
            log.info("Received task creation request: {}", taskData);

            String email = tokenService.getEmailFromToken(request);
            log.info("User email from token: {}", email);
            
            HashMap<String, Object> response = new HashMap<>();

            if(taskData != null && userService.checkUserExists(email)) {
                log.info("Task data and user are valid");

                User user = userService.getUser(email);
                log.info("Found user: {}", user);
                
                boolean taskCreated = taskService.setTask(taskData, user.getId(), email);
                log.info("Task creation result: {}", taskCreated);

                if(taskCreated){
                    log.info("Successfully set task");
                    response.put("message", "Successfully set task");
                    response.put("status", "success");
                    response.put("Create by", user.getFullName());
                    return new ResponseEntity<>(response, HttpStatus.OK);
                } else {
                    log.error("Task service returned false");
                    response.put("message", "Failed to create task");
                    response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                log.error("Invalid request - taskData: {}, userExists: {}", taskData != null, userService.checkUserExists(email));
                response.put("message", "Invalid request data or user not found");
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }catch (Exception e) {
            log.error("Exception in setTask: ", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/clone/{section-id}")
    public ResponseEntity<?> cloneTaskToSection(
            HttpServletRequest request,
            @PathVariable("section-id") String sectionId,
            @RequestBody CloneTaskRequest body
    ) {
        try {
            HashMap<String, Object> response = new HashMap<>();
            if (body == null) {
                response.put("message", "Request body is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Build new Task from body
            Task clone = new Task();

            clone.setTaskName(body.getTaskName());
            clone.setCreatedId(body.getCreatedId());
            clone.setProjectIds(body.getProjectIds());
            clone.setCompleted(body.getCompleted() != null ? body.getCompleted() : false);
            clone.setPriority(body.getPriority());
            clone.setStatus(body.getStatus());
            clone.setGroup(body.getGroup());
            clone.setDate(body.getDate());
            clone.setDescription(body.getDescription());
            clone.setStartDate(body.getStartDate() != null ? body.getStartDate() : body.getDate());
            clone.setEndDate(body.getEndDate());

            // Resolve assign emails -> user IDs
            List<String> assigneeIds = new ArrayList<>();
            if (body.getAssignId() != null) {
                for (String emailOrId : body.getAssignId()) {
                    String uid = null;
                    try {
                        User u = userService.getUser(emailOrId);
                        if (u != null) uid = u.getId();
                    } catch (Exception ignored) {}
                    assigneeIds.add(uid != null ? uid : emailOrId);
                }
            }
            clone.setAssignId(assigneeIds);

            // Save base task first to get ID
            Task saved = taskService.save(clone);

            // Attach provided subtask IDs as-is to the cloned task (no cloning of subtasks)
            if (body.getSubTasks() != null && !body.getSubTasks().isEmpty()) {
                saved.setSubTaskIds(new ArrayList<>(body.getSubTasks()));
                taskService.save(saved);
            }

            // Attach cloned task to the provided sectionId
            boolean sectionFound = false;
            List<com.upscale.upscale.entity.project.Project> projects = projectService.getProjects();
            for (com.upscale.upscale.entity.project.Project p : projects) {
                if (p.getSection() == null) continue;
                boolean changed = false;
                for (com.upscale.upscale.entity.project.Section s : p.getSection()) {
                    if (sectionId.equals(s.getId())) {
                        s.getTaskIds().add(saved.getId());
                        changed = true;
                        sectionFound = true;
                        break;
                    }
                }
                if (changed) projectService.save(p);
                if (sectionFound) break;
            }

            if (!sectionFound) {
                response.put("message", "Section not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            response.put("message", ">>> Task cloned successfully <<<");
            response.put("taskId", saved.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error cloning task into section {}", sectionId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Autowired
    private SubTaskService subTaskService;

    @PutMapping("/update/{task-id}")
    public ResponseEntity<?> updateTaskFields(
            HttpServletRequest request,
            @PathVariable("task-id") String taskId,
            @RequestBody UpdateTaskRequest body
    ) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if (body == null) {
                response.put("message", "Request body is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Try updating Task first
            Task task = taskService.getTask(taskId);
            if (task != null) {
                boolean ok = taskService.updateTaskFields(taskId, body, email);
                if (ok) {
                    response.put("message", ">>> Task updated successfully <<<");
                    response.put("type", "task");
                    response.put("taskId", taskId);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                response.put("message", "Failed to update task");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // If not a Task, fallback to SubTask
            Optional<SubTask> subTask = subTaskRepo.findById(taskId);
            if (subTask.isPresent()) {
                boolean ok = subTaskService.updateTaskFields(taskId, body, email);
                if (ok) {
                    response.put("message", ">>> Subtask updated successfully <<<");
                    response.put("type", "subtask");
                    response.put("taskId", taskId);
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                response.put("message", "Failed to update subtask");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Neither Task nor SubTask found
            response.put("message", "Task not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/schedule/{task-id}")
    public ResponseEntity<?> updateTaskSchedule(
            HttpServletRequest request,
            @PathVariable("task-id") String taskId,
            @RequestBody UpdateScheduleRequest body
    ) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            // Basic validation
            if (body.getStartDate() == null || body.getEndDate() == null) {
                response.put("message", "startDate and endDate are required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            if (body.getEndDate().before(body.getStartDate())) {
                response.put("message", "endDate must be after startDate");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            boolean ok = taskService.updateSchedule(taskId, body, email);
            if (ok) {
                response.put("message", ">>> Task schedule updated (recurrence applied) <<<");
                response.put("taskId", taskId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            response.put("message", "Task not found or failed to update");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get-task")
    public ResponseEntity<?> getTask(HttpServletRequest request) {
        try {

            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            TaskData[] taskData = taskService.getTaskDataByAssignId(email);

            // Also fetch subtasks assigned to this user
            List<Map<String, Object>> subTasks = new ArrayList<>();
            try {
                User user = userService.getUser(email);
                if (user != null) {
                    List<SubTask> assignedSubs = subTaskRepo.findByAssignId(user.getId());
                    if (assignedSubs != null) {
                        for (SubTask st : assignedSubs) {
                            Map<String, Object> stMap = new HashMap<>();
                            stMap.put("id", st.getId());
                            stMap.put("createdId", st.getCreatedId());
                            stMap.put("projectIds", st.getProjectIds());
                            stMap.put("taskName", st.getTaskName());
                            stMap.put("priority", st.getPriority());
                            stMap.put("status", st.getStatus());
                            stMap.put("group", st.getGroup());
                            stMap.put("date", st.getDate());
                            stMap.put("description", st.getDescription());
                            stMap.put("assignId", st.getAssignId());
                            stMap.put("startDate", st.getStartDate());
                            stMap.put("endDate", st.getEndDate());
                            stMap.put("completed", st.isCompleted());
                            subTasks.add(stMap);
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (taskData != null && taskData.length > 0) {
                response.put("tasks", taskData);
            } else {
                response.put("tasks", new ArrayList<>());
            }
            response.put("subTasks", subTasks);

            if ((taskData != null && taskData.length > 0) || !subTasks.isEmpty()) {
                response.put("status", "success");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "No task found");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get-assign-task")
    public ResponseEntity<?> getAssignTask(HttpServletRequest request) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();
            TaskData[] taskDataArr = taskService.getAssign(email);
            if(taskDataArr != null && taskDataArr.length > 0) {
                List<Map<String, Object>> enrichedTasks = new ArrayList<>();
                List<Map<String, Object>> aggregatedSubTasks = new ArrayList<>();
                for (TaskData td : taskDataArr) {
                    Task task = taskService.getTask(td.getId());
                    if (task == null) continue;

                    // Map assignees to names
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
                    taskMap.put("assignId", assigneeNames);
                    taskMap.put("startDate", task.getStartDate());
                    taskMap.put("endDate", task.getEndDate());
                    taskMap.put("completed", task.isCompleted());

                    // Collect subtasks separately (not nested under task)
                    if (task.getSubTaskIds() != null) {
                        for (String subTaskId : task.getSubTaskIds()) {
                            try {
                                SubTask st = subTaskRepo.findById(subTaskId).orElse(null);
                                if (st != null) {
                                    Map<String, Object> stMap = new HashMap<>();
                                    stMap.put("id", st.getId());
                                    stMap.put("createdId", st.getCreatedId());
                                    stMap.put("projectIds", st.getProjectIds());
                                    stMap.put("taskName", st.getTaskName());
                                    stMap.put("priority", st.getPriority());
                                    stMap.put("status", st.getStatus());
                                    stMap.put("group", st.getGroup());
                                    stMap.put("date", st.getDate());
                                    stMap.put("description", st.getDescription());
                                    stMap.put("assignId", st.getAssignId());
                                    stMap.put("startDate", st.getStartDate());
                                    stMap.put("endDate", st.getEndDate());
                                    stMap.put("completed", st.isCompleted());
                                    aggregatedSubTasks.add(stMap);
                                }
                            } catch (Exception ignored) {}
                        }
                    }

                    enrichedTasks.add(taskMap);
                }

                response.put("tasks", enrichedTasks);
                response.put("subTasks", aggregatedSubTasks);
                response.put("status", "success");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "No task found");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/complete/{task-id}")
    public ResponseEntity<?> complete(HttpServletRequest request, @PathVariable("task-id") String taskId) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            Task task = taskService.getTask(taskId);
            if (task != null) {
                taskService.updateTask(taskId);
                task.setCompleted(true);
                taskService.save(task);
                response.put("message", "Successfully completed task");
                response.put("status", "success");
                response.put("type", "task");
                response.put("id", task.getId());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            // Fallback: treat provided id as subtask id
            SubTask st = subTaskRepo.findById(taskId).orElse(null);
            if (st != null) {
                st.setCompleted(true);
                subTaskRepo.save(st);
                response.put("message", "Successfully completed subtask");
                response.put("status", "success");
                response.put("type", "subtask");
                response.put("id", st.getId());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            response.put("message", "Task or Subtask not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/{task-id}")
    public ResponseEntity<?> deleteTask(HttpServletRequest request, @PathVariable("task-id") String taskId) {

        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if(taskService.deleteTask(taskId)) {
                response.put("message", "Successfully deleted");
                response.put("status", "success");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "Failed to delete task");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create-name")
    public ResponseEntity<?> createTaskName(HttpServletRequest request, @RequestBody HashMap<String, String> payload) {
        try{

            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if(!payload.isEmpty()){

                String taskName = payload.get("taskName");
                String sectionId = payload.get("sectionId");

                if(taskService.addTaskToProject(taskName, sectionId)) {

                    response.put("taskName", taskName);
                    response.put("status", "created");
                    log.info("Successfully created task: {}",taskName);
                    return new ResponseEntity<>(response, HttpStatus.CREATED);
                }
                else{
                    response.put("message", "Failed to create task");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }


            }
            else{
                log.error("Task name is required");
                response.put("message", "Task name is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);

            }

        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create-task/details/{task-id}")
    public ResponseEntity<?> createTaskDetails(
            HttpServletRequest request,
            @PathVariable("task-id") String taskId,
            @RequestBody TaskData taskData
    ) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            Task task = taskService.getTask(taskId);
            if (task == null) {
                response.put("message", "Task not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (taskService.updateTaskToProject(taskId, taskData)) {
                response.put("message", "Task updated and moved to section successfully");
                response.put("taskId", task.getId());
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "Failed to update task");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
