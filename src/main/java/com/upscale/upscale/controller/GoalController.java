package com.upscale.upscale.controller;


import com.upscale.upscale.dto.GoalData;
import com.upscale.upscale.service.GoalService;
import com.upscale.upscale.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

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

    @GetMapping("/get-goal")
    public ResponseEntity<?> getGoal(HttpServletRequest request) {

        try{

            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            GoalData goalData = goalService.getGoal(emailId);
            if(goalData != null){
                response.put("status", "success");
                response.put("Data",goalData);
                log.info("Goal get successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("status", "error");
                log.info("Goal get failed");
                return new ResponseEntity<>(response, HttpStatus.NO_CONTENT);
            }

        }catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
