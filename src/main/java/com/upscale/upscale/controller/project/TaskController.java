package com.upscale.upscale.controller.project;


import com.upscale.upscale.dto.task.TaskData;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.service.project.ProjectService;
import com.upscale.upscale.service.project.TaskService;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

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

    @GetMapping("/get-task")
    public ResponseEntity<?> getTask(HttpServletRequest request) {
        try {

            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            TaskData[] taskData = taskService.getAll(email);

            if(taskData != null && taskData.length > 0) {
                response.put("tasks", taskData);
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

    @GetMapping("/get-assign-task")
    public ResponseEntity<?> getAssignTask(HttpServletRequest request) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();
            TaskData[] taskData = taskService.getAll(email);
            if(taskData != null && taskData.length > 0) {
                response.put("tasks", taskData);
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
            if(task != null) {
                taskService.updateTask(taskId);
                task.setCompleted(true);
                taskService.save(task);
                response.put("message", "Successfully completed");
                response.put("status", "success");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "Failed to complete task");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
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

            if(taskService.updateTaskToProject(taskId,taskData)){
                response.put("message", "Task updated and moved to section successfully");
                response.put("taskId", task.getId());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "Failed to update task");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
