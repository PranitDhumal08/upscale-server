package com.upscale.upscale.controller.project;

import com.upscale.upscale.dto.task.SubTaskData;
import com.upscale.upscale.entity.project.SubTask;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.project.SubTaskService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class SubTaskController {

    @Autowired
    private SubTaskService subTaskService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/{parent-task-id}/task/subtask/{project-id}")
    public ResponseEntity<?> createSubTask(
            HttpServletRequest request,
            @PathVariable("parent-task-id") String parentTaskId,
            @PathVariable("project-id") String projectId,
            @RequestBody SubTaskData subTaskData
    ) {
        try {
            String creatorEmail = tokenService.getEmailFromToken(request);
            if (subTaskData == null || subTaskData.getTaskName() == null || subTaskData.getTaskName().isBlank()) {
                return new ResponseEntity<>("taskName is required", HttpStatus.BAD_REQUEST);
            }

            // enforce path-provided linkage
            subTaskData.setParentTaskId(parentTaskId);
            subTaskData.setProjectId(projectId);

            SubTask saved = subTaskService.createSubTask(subTaskData, creatorEmail);

            HashMap<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("subTaskId", saved.getId());
            resp.put("message", "Subtask created and linked to parent");
            return new ResponseEntity<>(resp, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Failed to create subtask", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/subtask/complete/{subtask-id}")
    public ResponseEntity<?> updateSubTaskCompleted(
            @PathVariable("subtask-id") String subTaskId) {
        try {

            SubTask updated = subTaskService.updateCompleted(subTaskId, true);
            if (updated == null) {
                return new ResponseEntity<>("Subtask not found", HttpStatus.NOT_FOUND);
            }
            HashMap<String, Object> resp = new HashMap<>();
            resp.put("status", "success");
            resp.put("message", "Subtask completion updated");
            resp.put("subTaskId", updated.getId());
            resp.put("completed", updated.isCompleted());
            return new ResponseEntity<>(resp, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to update subtask completed status", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
