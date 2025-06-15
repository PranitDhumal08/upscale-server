package com.upscale.upscale.controller;


import com.upscale.upscale.dto.TaskData;
import com.upscale.upscale.entity.Task;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.service.TaskService;
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


    @PostMapping("/set-task")
    public ResponseEntity<?> setTask(HttpServletRequest request, @RequestBody TaskData taskData) {
        try {

            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if(taskData != null && userService.checkUserExists(email)) {

                User user = userService.getUser(email);
                if(taskService.setTask(taskData, user.getId(), email)){

                    log.info("Successfully set task");
                    response.put("message", "Successfully set task");
                    response.put("status", "success");
                    response.put("Create by", user.getFullName());
                    return new ResponseEntity<>(response, HttpStatus.OK);

                }
            }
            else{
                response.put("message", "Failed to set task");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>("Failed to set task", HttpStatus.INTERNAL_SERVER_ERROR);
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

    @PostMapping("/complete/{task-id}")
    public ResponseEntity<?> complete(HttpServletRequest request, @PathVariable("task-id") String taskId) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            Task task = taskService.getTask(taskId);
            if(task != null) {
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
}
