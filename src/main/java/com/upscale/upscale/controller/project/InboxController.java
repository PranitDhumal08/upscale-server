package com.upscale.upscale.controller.project;


import com.upscale.upscale.dto.project.InboxData;
import com.upscale.upscale.service.project.InboxService;
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
@RequestMapping("/api/inbox")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class InboxController {

    @Autowired
    private InboxService inboxService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/get-inbox" )
    public ResponseEntity<?> getAllInbox(HttpServletRequest request,
                                         @RequestParam(value = "type", required = false) String type) {
        try{

            String emailId = tokenService.getEmailFromToken(request);

            List<InboxData> inboxDataList = (type == null || type.isBlank())
                    ? inboxService.getInbox(emailId)
                    : inboxService.getInbox(emailId, type);

            HashMap<String,Object> response = new HashMap<>();
            if(!inboxDataList.isEmpty()){

                response.put("message",">>> Inbox fetched successfully <<<");
                response.put("Data",inboxDataList);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message",">>> Inbox not found <<<");
                response.put("Data", new ArrayList<>());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }


        }catch (Exception e) {
            return new ResponseEntity<>(">>> Failed to get inbox <<<", HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markInboxAsRead(@PathVariable("id") String id, HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            boolean ok = inboxService.markRead(id, emailId);
            if (ok) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", ">>> Inbox marked as read <<<");
                response.put("id", id);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            return new ResponseEntity<>(">>> Unable to mark inbox as read <<<", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(">>> Failed to mark inbox as read <<<", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archiveInbox(@PathVariable("id") String id, HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            boolean ok = inboxService.archive(id, emailId);
            if (ok) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", ">>> Inbox archived <<<");
                response.put("id", id);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            return new ResponseEntity<>(">>> Unable to archive inbox <<<", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(">>> Failed to archive inbox <<<", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<?> unarchiveInbox(@PathVariable("id") String id, HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            boolean ok = inboxService.unarchive(id, emailId);
            if (ok) {
                HashMap<String, Object> response = new HashMap<>();
                response.put("message", ">>> Inbox unarchived <<<");
                response.put("id", id);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            return new ResponseEntity<>(">>> Unable to unarchive inbox <<<", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(">>> Failed to unarchive inbox <<<", HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-archived")
    public ResponseEntity<?> getArchivedInbox(HttpServletRequest request,
                                              @RequestParam(value = "type", required = false) String type) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            List<InboxData> data = inboxService.getArchivedInbox(emailId, type);
            HashMap<String,Object> response = new HashMap<>();
            response.put("message", ">>> Archived inbox fetched successfully <<<");
            response.put("Data", data);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(">>> Failed to get archived inbox <<<", HttpStatus.BAD_REQUEST);
        }
    }

}
