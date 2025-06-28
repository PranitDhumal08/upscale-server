package com.upscale.upscale.controller;


import com.upscale.upscale.dto.MessageData;
import com.upscale.upscale.entity.Message;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.service.MessageService;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.*;

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

            List<Message> messages = messageService.getMessagesForUser(projectId);

            if(messages != null && messages.size() > 0) {
                response.put("status", "success");
                response.put("messages", messages);

                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{

                List<Message> messagesForUser = messageService.getMessagesForUser(email);

                if(messagesForUser != null && messagesForUser.size() > 0) {
                    response.put("status", "success");
                    response.put("messages", messagesForUser);
                    return new ResponseEntity<>(response, HttpStatus.OK);

                }
                else{
                    response.put("status", "failure");
                    response.put("message", "No such message.");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

        }catch (Exception e) {
            log.error("Error while getting message", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}

