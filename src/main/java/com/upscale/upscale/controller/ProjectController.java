package com.upscale.upscale.controller;

import com.upscale.upscale.dto.ProjectCreate;
import com.upscale.upscale.dto.ProjectData;
import com.upscale.upscale.service.ProjectService;
import com.upscale.upscale.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/project")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private TokenService tokenService;

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

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(HttpServletRequest request) {
        try{
            String email = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();
            if(projectService.getProject(email) != null){

                ProjectData projectData = projectService.getInfo(email);

                response.put("Data",projectData);
                return new ResponseEntity<>(response, HttpStatus.OK);

            }
            response.put("message", "Project not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);


        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
