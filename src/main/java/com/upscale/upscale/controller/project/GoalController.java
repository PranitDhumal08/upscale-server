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
}
