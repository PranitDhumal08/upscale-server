package com.upscale.upscale.controller.project;


import com.upscale.upscale.dto.project.GoalData;
import com.upscale.upscale.service.project.GoalService;
import com.upscale.upscale.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/goal")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class GoalController {

    @Autowired
    private GoalService goalService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/set-goal")
    public ResponseEntity<?> setGoal(HttpServletRequest request, @RequestBody GoalData goalData) {

        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            if(goalData != null){
                goalData.setGoalOwner(emailId);
                if(goalService.setGoal(emailId, goalData)){
                    response.put("status", "success");
                    response.put("Data",goalData);
                    log.info("Goal set successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else{
                    response.put("status", "error");
                    response.put("Data",goalData);
                    log.info("Goal set failed");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }else{
                response.put("status", "error");
                response.put("Data",goalData);
                log.info("Goal set failed");
                return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
            }


        }catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-team-goal")
    public ResponseEntity<?> getGoal(HttpServletRequest request) {

        try{

            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            List<GoalData> goalData = goalService.getTeamGoal(emailId);
            if(!goalData.isEmpty()){
                response.put("status", "success");
                response.put("Data",goalData);
                log.info("Goal get successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("status", "error");
                response.put("message", "No goals found");
                log.info("Goal get failed");
                return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
            }

        }catch (Exception e) {
            HashMap<String,Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-my-goals")
    public ResponseEntity<?> getMyGoals(HttpServletRequest request) {

        try{

            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            List<GoalData> goalDataList = goalService.getMyGoals(emailId);
            if(goalDataList != null && !goalDataList.isEmpty()){
                response.put("status", "success");
                response.put("Data", goalDataList);
                response.put("count", goalDataList.size());
                log.info("All goals retrieved successfully. Count: {}", goalDataList.size());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("status", "error");
                response.put("message", "No goals found");
                response.put("count", 0);
                log.info("No goals found for user");
                return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
            }

        }catch (Exception e) {
            HashMap<String,Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{parent-goal-id}/create-subgoal")
    public ResponseEntity<?> createSubGoal(@PathVariable("parent-goal-id") String parentGoalId, HttpServletRequest request, @RequestBody GoalData goalData) {

        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            if(goalData != null && parentGoalId != null && !parentGoalId.isEmpty()){
                // Set the goal owner to current user
                goalData.setGoalOwner(emailId);
                goalData.setParentGoalId(parentGoalId);
                
                if(goalService.setSubGoal(parentGoalId, goalData)){
                    response.put("status", "success");
                    response.put("message", "Sub-goal created successfully");
                    response.put("parentGoalId", parentGoalId);
                    response.put("Data", goalData);
                    log.info("Sub-goal '{}' created successfully for parent goal: {}", goalData.getGoalTitle(), parentGoalId);
                    return new ResponseEntity<>(response, HttpStatus.CREATED);
                }else{
                    response.put("status", "error");
                    response.put("message", "Failed to create sub-goal. Parent goal not found or invalid data.");
                    log.error("Failed to create sub-goal for parent goal: {}", parentGoalId);
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
            }else{
                response.put("status", "error");
                response.put("message", "Invalid request data. Parent goal ID and sub-goal data are required.");
                log.error("Invalid request data for sub-goal creation");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }catch (Exception e) {
            HashMap<String,Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error creating sub-goal: " + e.getMessage());
            log.error("Exception while creating sub-goal: ", e);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get-all-goals-with-children")
    public ResponseEntity<?> getAllGoalsWithChildren(HttpServletRequest request) {

        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            List<GoalData> allGoalsWithChildren = goalService.getAllGoalsWithChildren(emailId);
            
            if (allGoalsWithChildren != null && !allGoalsWithChildren.isEmpty()) {
                response.put("status", "success");
                response.put("message", "All goals with sub-goals retrieved successfully");
                response.put("Data", allGoalsWithChildren);
                response.put("count", allGoalsWithChildren.size());
                
                // Calculate total sub-goals count
                int totalSubGoals = allGoalsWithChildren.stream()
                    .mapToInt(goal -> goal.getChildren().size())
                    .sum();
                response.put("totalSubGoals", totalSubGoals);
                
                log.info("Retrieved {} parent goals with {} total sub-goals for user {}", 
                        allGoalsWithChildren.size(), totalSubGoals, emailId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("status", "success");
                response.put("message", "No goals found");
                response.put("Data", new ArrayList<>());
                response.put("count", 0);
                response.put("totalSubGoals", 0);
                log.info("No goals found for user {}", emailId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

        } catch (Exception e) {
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error retrieving goals: " + e.getMessage());
            log.error("Exception while retrieving all goals with children: ", e);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
