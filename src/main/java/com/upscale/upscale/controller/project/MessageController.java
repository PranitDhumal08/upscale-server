package com.upscale.upscale.controller.project;


import com.upscale.upscale.dto.project.MessageData;
import com.upscale.upscale.entity.project.Message;
import com.upscale.upscale.service.project.MessageService;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
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
@RequestMapping("/api/message")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserService userService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(HttpServletRequest request, @RequestBody MessageData messageData) {
        HashMap<String, Object> response = new HashMap<>();
        try {
            String email = tokenService.getEmailFromToken(request);
            boolean sent = messageService.sendMessage(email, messageData);

            if (sent) {
                response.put("status", "success");
                response.put("message", "Message sent successfully.");

                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("status", "failure");
                response.put("message", "Failed to send message.");

                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            log.error("Error while sending message", e);
            response.put("status", "error");
            response.put("message", "An unexpected error occurred.");

            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }

    @GetMapping("/get-message/{project-id}")
    public ResponseEntity<?> getMessage(HttpServletRequest request, @PathVariable("project-id") String projectId) {

        try{
            String email = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();

            // Get enriched messages for the specific project (includes sender names)
            List<java.util.Map<String, Object>> enrichedMessages = messageService.getEnrichedMessagesForProject(projectId);

            if(enrichedMessages != null && !enrichedMessages.isEmpty()) {
                response.put("status", "success");
                response.put("messages", enrichedMessages);
                response.put("projectId", projectId);
                response.put("count", enrichedMessages.size());

                log.info("Retrieved {} enriched messages for project {} requested by user {}", 
                        enrichedMessages.size(), projectId, email);

                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("status", "success");
                response.put("messages", new ArrayList<>());
                response.put("projectId", projectId);
                response.put("count", 0);
                response.put("message", "No messages found for this project.");

                log.info("No messages found for project {} requested by user {}", projectId, email);

                return new ResponseEntity<>(response, HttpStatus.OK);
            }

        }catch (Exception e) {
            log.error("Error while getting messages for project {}: {}", projectId, e.getMessage());
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "An error occurred while retrieving messages.");
            errorResponse.put("projectId", projectId);
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}

