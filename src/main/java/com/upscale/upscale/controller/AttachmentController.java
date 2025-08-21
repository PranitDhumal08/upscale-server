package com.upscale.upscale.controller;

import com.upscale.upscale.entity.project.FileAttachment;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.project.FileAttachmentService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.entity.user.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class AttachmentController {

    @Autowired
    private FileAttachmentService fileAttachmentService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserService userService;

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> upload(HttpServletRequest request,
            @RequestParam("projectId") String projectId,
            @RequestParam("receiverEmails") String receiverEmailsCsv,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "fileName", required = false) String fileName
    ) {
        try {

            String senderEmail = tokenService.getEmailFromToken(request);

            List<String> receiverEmails = receiverEmailsCsv == null || receiverEmailsCsv.isBlank()
                    ? List.of()
                    : Arrays.stream(receiverEmailsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

            System.out.println(receiverEmails);

            FileAttachment saved = fileAttachmentService.upload(
                    projectId,
                    senderEmail,
                    receiverEmails,
                    file,
                    description,
                    fileName
            );

            return new ResponseEntity<>(saved, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            log.warn("Upload validation failed: {}", ex.getMessage());
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Upload failed", e);
            return new ResponseEntity<>("Upload failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttachment(HttpServletRequest request, @PathVariable("id") String id) {
        try {
            String requesterEmail = tokenService.getEmailFromToken(request);
            fileAttachmentService.deleteAttachment(id, requesterEmail);
            return new ResponseEntity<>(Map.of("message", "Deleted", "status", "success"), HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Delete attachment failed", e);
            return new ResponseEntity<>("Failed to delete attachment", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<?> editAttachment(
            HttpServletRequest request,
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> payload
    ) {
        try {
            String requesterEmail = tokenService.getEmailFromToken(request);

            // Accept either a CSV string or an array for receiverEmails
            Object recvObj = payload.get("receiverEmails");
            List<String> receiverEmails;
            if (recvObj instanceof String) {
                String csv = (String) recvObj;
                receiverEmails = (csv == null || csv.isBlank()) ? List.of() : Arrays.stream(csv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            } else if (recvObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> raw = (List<Object>) recvObj;
                receiverEmails = raw.stream().filter(Objects::nonNull).map(Object::toString)
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
            } else {
                receiverEmails = List.of();
            }

            String description = null;
            if (payload.containsKey("description") && payload.get("description") != null) {
                description = payload.get("description").toString();
            }

            FileAttachment updated = fileAttachmentService.updateRecipientsAndDescription(
                    id,
                    requesterEmail,
                    receiverEmails,
                    description
            );

            // Minimal response
            Map<String, Object> resp = Map.of(
                    "id", updated.getId(),
                    "projectId", updated.getProjectId(),
                    "description", updated.getDescription(),
                    "receiverIds", updated.getReceiverIds()
            );
            return new ResponseEntity<>(resp, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Edit attachment failed", e);
            return new ResponseEntity<>("Failed to edit attachment", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/received/{project-id}")
    public ResponseEntity<?> listReceived(HttpServletRequest request, @PathVariable("project-id") String projectId) {
        try {
            String email = tokenService.getEmailFromToken(request);
            User user = userService.getUser(email);
            if (user == null) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            // Get detailed results filtered by project
            List<Map<String, Object>> items = fileAttachmentService.listDetailedForUserAndProject(user.getId(), projectId);
            // Shape response: include sender and receivers with only id, name, email; include core file fields
            List<Map<String, Object>> response = items.stream().map(m -> {
                String dn = (String) m.get("displayName");
                String on = (String) m.get("originalFileName");
                String fileName = (dn != null && !dn.isBlank()) ? dn : on;
                Map<String, Object> sender = (Map<String, Object>) m.get("sender");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> receivers = (List<Map<String, Object>>) m.get("receivers");

                // Whitelist sender fields
                Map<String, Object> senderMin = sender == null ? Map.of() : Map.of(
                        "id", sender.get("id"),
                        "name", sender.get("name"),
                        "email", sender.get("email")
                );
                // Whitelist receiver fields
                List<Map<String, Object>> receiversMin = receivers == null ? List.of() : receivers.stream().map(r -> Map.of(
                        "id", r.get("id"),
                        "name", r.get("name"),
                        "email", r.get("email")
                )).collect(Collectors.toList());

                return Map.of(
                        "fileId", m.get("id"),
                        "projectId", m.get("projectId"),
                        "uploadDate", m.get("uploadDate"),
                        "description", m.get("description"),
                        "fileType", m.get("fileType"),
                        "role", m.get("role"),
                        "fileName", fileName,
                        "sender", senderMin,
                        "receivers", receiversMin
                );
            }).collect(Collectors.toList());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("List received attachments failed", e);
            return new ResponseEntity<>("Failed to list attachments", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> downloadById(@PathVariable("id") String id, HttpServletRequest request) {
        try {
            String email = tokenService.getEmailFromToken(request);
            User user = userService.getUser(email);
            if (user == null) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            return fileAttachmentService.getIfAuthorized(id, user.getId())
                    .<ResponseEntity<?>>map(att -> {
                        String mime = att.getMimeType() != null ? att.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
                        HttpHeaders headers = new HttpHeaders();
                        headers.set(HttpHeaders.CONTENT_TYPE, mime);
                        // default to attachment download, preserving original filename if present
                        String filename = att.getDisplayName() != null && !att.getDisplayName().isBlank()
                                ? att.getDisplayName()
                                : (att.getOriginalFileName() != null ? att.getOriginalFileName() : att.getFileName());
                        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
                        return new ResponseEntity<>(att.getFileData(), headers, HttpStatus.OK);
                    })
                    .orElseGet(() -> new ResponseEntity<>("Not found or not authorized", HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            log.error("Download attachment failed", e);
            return new ResponseEntity<>("Failed to download attachment", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
