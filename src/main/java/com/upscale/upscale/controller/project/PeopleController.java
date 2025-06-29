package com.upscale.upscale.controller.project;

import com.upscale.upscale.dto.project.PeopleInvite;
import com.upscale.upscale.service.project.PeopleService;
import com.upscale.upscale.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/people")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class PeopleController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PeopleService peopleService;

    @PostMapping("/invite")
    public ResponseEntity<?> sendInvite(HttpServletRequest request, @RequestBody PeopleInvite peopleInvite) {
        try {

            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            if(peopleInvite != null){

                log.info("Sending Invite to People");


                if(peopleService.setPeople(peopleInvite, emailId)){
                    response.put("message",">>> Invite sent successfully <<<");
                    response.put("send to",peopleInvite.getReceiverEmailId());
                    response.put("project",peopleInvite.getProjectId());
                    log.info("Invite sent successfully: " + emailId + " to " + peopleInvite.getReceiverEmailId());
                    return new ResponseEntity<>(response, org.springframework.http.HttpStatus.OK);
                }
                else{
                    response.put("message",">>> Invite failed to send <<<");
                    response.put("send to",peopleInvite.getReceiverEmailId());
                    response.put("project",peopleInvite.getProjectId());
                    log.info("Invite failed to send: " + emailId + " to " + peopleInvite.getReceiverEmailId());
                    return new ResponseEntity<>(response, org.springframework.http.HttpStatus.BAD_REQUEST);
                }
            }
            response.put("message",">>> Invite failed to send <<<");
            response.put("send to",peopleInvite.getReceiverEmailId());
            response.put("project",peopleInvite.getProjectId());
            log.info("Invite failed to send: " + emailId + " to " + peopleInvite.getReceiverEmailId());
            return new ResponseEntity<>(response, org.springframework.http.HttpStatus.BAD_REQUEST);
        }catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
