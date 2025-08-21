package com.upscale.upscale.service.project;

import com.upscale.upscale.entity.project.FileAttachment;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.FileAttachmentRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class FileAttachmentService {

    @Autowired
    private FileAttachmentRepo fileAttachmentRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "xls", "xlsx", "ppt", "pptx", "jpg", "jpeg", "png", "gif", "bmp", "webp"
    ));

    public FileAttachment upload(String projectId,
                                 String senderEmail,
                                 List<String> receiverEmails,
                                 MultipartFile file,
                                 String description,
                                 String displayName) throws IOException {
        // Validate project exists (could also be portfolio ID but here we ensure project existence)
        Project project = projectService.getProject(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Invalid projectId: " + projectId);
        }

        // Resolve sender
        User sender = userService.getUser(senderEmail);
        if (sender == null) {
            throw new IllegalArgumentException("Sender not found for email: " + senderEmail);
        }

        // Resolve receivers
        List<String> receiverIds = new ArrayList<>();
        if (receiverEmails != null) {
            for (String email : receiverEmails) {

                if (email == null || email.isBlank()) continue;

                User u = userService.getUser(email.trim());
                if (u != null) {
                    // avoid duplicates and skipping the sender
                    if (!u.getId().equals(sender.getId()) && !receiverIds.contains(u.getId())) {
                        receiverIds.add(u.getId());
                        log.info("user found for email: " + email);
                    }
                } else {
                    log.warn("Receiver not found for email: {}", email);
                }
            }
        }

        validateFileType(file);

        FileAttachment att = new FileAttachment();
        att.setProjectId(projectId);
        att.setSenderId(sender.getId());
        att.setReceiverIds(receiverIds);
        att.setOriginalFileName(file.getOriginalFilename());
        att.setFileName(generateStoredName(file.getOriginalFilename()));
        att.setMimeType(file.getContentType());
        att.setFileSize(file.getSize());
        att.setFileData(file.getBytes());
        att.setUploadDate(new Date());
        att.setDescription(description);
        att.setFileType(resolveLogicalType(file)); // PDF | EXCEL | PPT | IMAGE
        if (displayName != null && !displayName.isBlank()) {
            // Ensure displayName has an extension if user didn't provide
            String ext = getExtension(att.getOriginalFileName());
            String dn = displayName;
            if (ext != null && !ext.isBlank() && !dn.toLowerCase().endsWith(("." + ext).toLowerCase())) {
                dn = dn + "." + ext;
            }
            att.setDisplayName(dn);
        }

        FileAttachment saved = fileAttachmentRepo.save(att);
        log.info("Stored attachment {} ({} bytes) for project {} from {} to {} receivers",
                saved.getId(), saved.getFileSize(), projectId, senderEmail, receiverIds.size());

        return saved;
    }

    /**
     * List attachments where the given userId is a receiver.
     * Returns lightweight maps: id, description, fileType, originalFileName, uploadDate, projectId
     */
    public List<Map<String, Object>> listForReceiver(String userId) {
        List<FileAttachment> items = fileAttachmentRepo.findByReceiverIdsContaining(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FileAttachment fa : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", fa.getId());
            m.put("description", fa.getDescription());
            m.put("fileType", fa.getFileType());
            m.put("originalFileName", fa.getOriginalFileName());
            m.put("displayName", fa.getDisplayName());
            m.put("uploadDate", fa.getUploadDate());
            m.put("projectId", fa.getProjectId());
            result.add(m);
        }
        return result;
    }

    /**
     * List attachments where the user is sender and/or receiver, labeled with role.
     * role: RECEIVER | SENDER | BOTH
     */
    public List<Map<String, Object>> listForUserWithRole(String userId) {
        List<FileAttachment> asReceiver = fileAttachmentRepo.findByReceiverIdsContaining(userId);
        List<FileAttachment> asSender = fileAttachmentRepo.findBySenderId(userId);

        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();

        for (FileAttachment fa : asReceiver) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", fa.getId());
            m.put("description", fa.getDescription());
            m.put("fileType", fa.getFileType());
            m.put("originalFileName", fa.getOriginalFileName());
            m.put("displayName", fa.getDisplayName());
            m.put("uploadDate", fa.getUploadDate());
            m.put("projectId", fa.getProjectId());
            m.put("role", "RECEIVER");
            byId.put(fa.getId(), m);
        }

        for (FileAttachment fa : asSender) {
            Map<String, Object> existing = byId.get(fa.getId());
            if (existing != null) {
                existing.put("role", "BOTH");
            } else {
                Map<String, Object> m = new HashMap<>();
                m.put("id", fa.getId());
                m.put("description", fa.getDescription());
                m.put("fileType", fa.getFileType());
                m.put("originalFileName", fa.getOriginalFileName());
                m.put("displayName", fa.getDisplayName());
                m.put("uploadDate", fa.getUploadDate());
                m.put("projectId", fa.getProjectId());
                m.put("role", "SENDER");
                byId.put(fa.getId(), m);
            }
        }

        return new ArrayList<>(byId.values());
    }

    /**
     * Fetch attachment if the user is authorized (sender or receiver).
     */
    public Optional<FileAttachment> getIfAuthorized(String attachmentId, String userId) {
        Optional<FileAttachment> opt = fileAttachmentRepo.findById(attachmentId);
        if (opt.isEmpty()) return Optional.empty();
        FileAttachment fa = opt.get();
        boolean isSender = userId != null && userId.equals(fa.getSenderId());
        boolean isReceiver = userId != null && fa.getReceiverIds() != null && fa.getReceiverIds().contains(userId);
        if (isSender || isReceiver) return opt;
        return Optional.empty();
    }

    /**
     * Return detailed attachments for a user and project with sender/receivers info.
     * Each item contains: id, description, fileType, projectId, role, fileName info,
     * sender {id, name, email}, receivers [{id, name, email}]
     */
    public List<Map<String, Object>> listDetailedForUserAndProject(String userId, String projectId) {
        List<FileAttachment> asReceiver = fileAttachmentRepo.findByReceiverIdsContaining(userId);
        List<FileAttachment> asSender = fileAttachmentRepo.findBySenderId(userId);

        // Merge by ID with role aggregation
        Map<String, FileAttachment> byId = new LinkedHashMap<>();
        Map<String, String> roleById = new HashMap<>();

        for (FileAttachment fa : asReceiver) {
            if (projectId != null && !projectId.equals(fa.getProjectId())) continue;
            byId.put(fa.getId(), fa);
            roleById.put(fa.getId(), "RECEIVER");
        }
        for (FileAttachment fa : asSender) {
            if (projectId != null && !projectId.equals(fa.getProjectId())) continue;
            String existing = roleById.get(fa.getId());
            if (existing != null && !"SENDER".equals(existing)) {
                roleById.put(fa.getId(), "BOTH");
            } else {
                roleById.put(fa.getId(), "SENDER");
            }
            byId.putIfAbsent(fa.getId(), fa);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, FileAttachment> entry : byId.entrySet()) {
            FileAttachment fa = entry.getValue();

            Map<String, Object> item = new HashMap<>();
            item.put("id", fa.getId());
            item.put("description", fa.getDescription());
            item.put("fileType", fa.getFileType());
            item.put("uploadDate", fa.getUploadDate());
            item.put("projectId", fa.getProjectId());
            item.put("role", roleById.get(fa.getId()));
            item.put("originalFileName", fa.getOriginalFileName());
            item.put("displayName", fa.getDisplayName());

            // Sender info
            Map<String, Object> senderMap = new HashMap<>();
            if (fa.getSenderId() != null) {
                User sender = userService.getUserById(fa.getSenderId());
                if (sender != null) {
                    senderMap.put("id", sender.getId());
                    senderMap.put("name", sender.getFullName());
                    senderMap.put("email", sender.getEmailId());
                } else {
                    senderMap.put("id", fa.getSenderId());
                }
            }
            item.put("sender", senderMap);

            // Receivers info
            List<Map<String, Object>> receiversList = new ArrayList<>();
            if (fa.getReceiverIds() != null) {
                for (String rid : fa.getReceiverIds()) {
                    if (rid == null) continue;
                    Map<String, Object> rmap = new HashMap<>();
                    User u = userService.getUserById(rid);
                    if (u != null) {
                        rmap.put("id", u.getId());
                        rmap.put("name", u.getFullName());
                        rmap.put("email", u.getEmailId());

                        log.info("user name is"+u.getFullName());
                    } else {
                        rmap.put("id", rid);

                    }
                    receiversList.add(rmap);
                }
            }
            item.put("receivers", receiversList);

            result.add(item);
        }

        return result;
    }

    private void validateFileType(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null) throw new IllegalArgumentException("File must have a name");
        String ext = getExtension(original).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("File type not allowed: ." + ext);
        }
    }

    private String resolveLogicalType(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename()).toLowerCase();
        switch (ext) {
            case "pdf":
                return "PDF";
            case "xls":
            case "xlsx":
                return "EXCEL";
            case "ppt":
            case "pptx":
                return "PPT";
            default:
                return "IMAGE";
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.')
                ;
        return (i >= 0 && i < filename.length() - 1) ? filename.substring(i + 1) : "";
    }

    private String generateStoredName(String original) {
        String uuid = UUID.randomUUID().toString();
        String ext = getExtension(original);
        return ext.isEmpty() ? uuid : uuid + "." + ext;
    }
}
