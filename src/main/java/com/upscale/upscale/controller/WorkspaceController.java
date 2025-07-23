package com.upscale.upscale.controller;

import com.upscale.upscale.dto.workspace.Entry;
import com.upscale.upscale.entity.workspace.Workspace;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.Workspace.CuratedWork;
import com.upscale.upscale.service.Workspace.WorkspaceService;
import com.upscale.upscale.service.project.ProjectService;
import com.upscale.upscale.service.project.TaskService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;

import java.text.SimpleDateFormat;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspace")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/get-overview")
    public ResponseEntity<?> getWorkspaceOverview(HttpServletRequest request) {

        String emailId = tokenService.getEmailFromToken(request);

        try {

            User user = userService.getUser(emailId);

            Workspace workspace = workspaceService.getWorkspace(user.getId());

            HashMap<String, Object> response = new HashMap<>();

            if(workspace != null) {
                //response.put("message", "Workspace overview");
                response.put("workspaceName", workspace.getName());
                response.put("workspaceDescription", workspace.getDescription());
                response.put("workspaceId", workspace.getId());
                List<String[]> members = new ArrayList<>();

                for(int i=0;i<workspace.getMembers().size();i++) {
                    String[] member = new String[3];
                    User userMember = userService.getUserById(workspace.getMembers().get(i));

                    member[0] = userMember.getFullName();
                    member[1] = userMember.getEmailId();
                    member[2] = userMember.getJobTitle();

                    members.add(member);
                }
                response.put("members", members);
                response.put("curatedWork",workspace.getCuratedWorkData());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "Workspace not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }



        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/get-overview/create-section/{section-name}")
    public ResponseEntity<?> getWorkspaceOverviewCreateSection(HttpServletRequest request, @PathVariable("section-name") String sectionName) {

        String emailId = tokenService.getEmailFromToken(request);

        try {

            HashMap<String, Object> response = new HashMap<>();
            User user = userService.getUser(emailId);
            Workspace workspace = workspaceService.getWorkspace(user.getId());

            HashMap<String, List<CuratedWork>> current_workspace = workspace.getCuratedWorkData();
            if(!current_workspace.containsKey(sectionName)) {

                current_workspace.put(sectionName.toLowerCase(), new ArrayList<>());
                workspace.setCuratedWorkData(current_workspace);

                workspaceService.save(workspace);
                log.info(String.format("Created workspace current work section with name %s", sectionName));
                response.put("message", "Workspace current work section created successfully");

                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{

                log.info(String.format("Workspace current work section with name %s already exists", sectionName));

                response.put("message", "Workspace current work section already exists");
                return new ResponseEntity<>(response, HttpStatus.CONFLICT);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }



    @PostMapping("/{section-name}/curated-work")
    public ResponseEntity<?> createWork(HttpServletRequest request, @PathVariable("section-name") String sectionName, @RequestBody CuratedWork curatedWork) {

        String emailId = tokenService.getEmailFromToken(request);

        try {
            HashMap<String, Object> response = new HashMap<>();

            User user = userService.getUser(emailId);

            if(workspaceService.setCuratedWork(sectionName, user.getId(), curatedWork)) {
                response.put("message", "Workspace updated successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "Workspace could not be updated");
                return new ResponseEntity<>(response, HttpStatus.CONFLICT);
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-members")
    public ResponseEntity<?> getWorkspaceMembers(HttpServletRequest request) {
        String emailId = tokenService.getEmailFromToken(request);

        try {
            User user = userService.getUser(emailId);

            Workspace workspace = workspaceService.getWorkspace(user.getId());

            HashMap<String, Object> response = new HashMap<>();

            if(workspace != null) {
                response.put("message", "Workspace found");
                response.put("workspaceName", workspace.getName());

                List<String> members = workspace.getMembers();

                List<HashMap<String,String>> memberMap = new ArrayList<>();

                for(String member : members) {
                    memberMap.add(workspaceService.getMemberInfo(member));
                }

                response.put("members", memberMap);

                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "Workspace not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("{old-section-name}/rename-to/{new-section-name}")
    public ResponseEntity<?> renameSectionName(HttpServletRequest request, @PathVariable("old-section-name") String oldSectionName, @PathVariable("new-section-name") String newSectionName) {
        String emailId = tokenService.getEmailFromToken(request);

        try {
            HashMap<String, Object> response = new HashMap<>();
            User user = userService.getUser(emailId);
            Workspace workspace = workspaceService.getWorkspace(user.getId());

            if(workspace != null) {

                HashMap<String,List<CuratedWork>> curatedWorkHashMap = workspace.getCuratedWorkData();

                if(curatedWorkHashMap.containsKey(oldSectionName)) {

                    List<CuratedWork> sectionData = curatedWorkHashMap.remove(oldSectionName);
                    curatedWorkHashMap.put(newSectionName, sectionData);

                    workspace.setCuratedWorkData(curatedWorkHashMap);

                    workspaceService.save(workspace);
                    response.put("message", "Workspace updated successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("message", "section not found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
            }else {
                response.put("message", "Workspace not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete-section/{section-name}")
    public ResponseEntity<?> deleteSection(HttpServletRequest request, @PathVariable("section-name") String sectionName) {
        String emailId = tokenService.getEmailFromToken(request);

        try {
            HashMap<String, Object> response = new HashMap<>();
            User user = userService.getUser(emailId);
            Workspace workspace = workspaceService.getWorkspace(user.getId());
            if(workspace != null) {
                HashMap<String,List<CuratedWork>> curatedWorkHashMap = workspace.getCuratedWorkData();

                if(curatedWorkHashMap.containsKey(sectionName)) {

                    curatedWorkHashMap.remove(sectionName);

                    workspace.setCuratedWorkData(curatedWorkHashMap);

                    workspaceService.save(workspace);
                    response.put("message", "Workspace section deleted successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("message", "section not found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }
            }
            else{
                response.put("message", "Workspace not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete/{section-name}/{project-id}")
    public ResponseEntity<?> deleteProjectId(HttpServletRequest request, @PathVariable("section-name") String sectionName, @PathVariable("project-id") String projectId) {

        String emailId = tokenService.getEmailFromToken(request);

        try {
            HashMap<String, Object> response = new HashMap<>();
            User user = userService.getUser(emailId);
            Workspace workspace = workspaceService.getWorkspace(user.getId());

            if(workspace != null) {

                HashMap<String,List<CuratedWork>> curatedWorkHashMap = workspace.getCuratedWorkData();

                if(curatedWorkHashMap.containsKey(sectionName)) {

                    List<CuratedWork> curatedWorkList = curatedWorkHashMap.get(sectionName);

                    boolean projectRemoved = curatedWorkList.removeIf(curatedWork -> curatedWork.getProjectId().equals(projectId));

                    if(projectRemoved) {
                        workspace.setCuratedWorkData(curatedWorkHashMap);
                        workspaceService.save(workspace);
                        response.put("message", "Workspace deleted successfully");
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }
                    else{
                        response.put("message", "project not found");
                        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                    }
                }
                else{
                    response.put("message", "Workspace not found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }

            }
            else{
                response.put("message", "Workspace not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        }catch (Exception e) {

            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all-work")
    public ResponseEntity<?> getAllWork(HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            User user = userService.getUser(emailId);

            HashMap<String, Object> response = new HashMap<>();

            if (user == null) {
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            HashMap<String, Object> dataMap = workspaceService.getAllwork(user.getId());

            if (dataMap == null || ((HashMap)dataMap.get("members")).isEmpty()) {
                response.put("message", "Workspace not found or is empty");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            response.put("message", "Workspace found");
            response.put("data", dataMap);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error(e.getMessage());
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An internal error occurred.");
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @GetMapping("/calendar-tasks")
    public ResponseEntity<?> getWorkspaceCalendarTasks(
            HttpServletRequest request,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end
    ) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            User user = userService.getUser(emailId);

            HashMap<String, Object> response = new HashMap<>();

            if (user == null) {
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            Workspace workspace = workspaceService.getWorkspace(user.getId());
            if (workspace == null) {
                response.put("message", "Workspace not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // Calendar data structure: date -> list of tasks
            HashMap<String, List<HashMap<String, Object>>> calendar = new HashMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            // Parse date range if provided
            Date startDate = null, endDate = null;
            try {
                if (start != null && !start.isEmpty()) {
                    startDate = dateFormat.parse(start);
                }
                if (end != null && !end.isEmpty()) {
                    endDate = dateFormat.parse(end);
                }
            } catch (Exception e) {
                log.warn("Invalid date format provided: start={}, end={}", start, end);
            }

            // Only get tasks from projects directly referenced in CuratedWorkData
            HashMap<String, List<CuratedWork>> curatedWork = workspace.getCuratedWorkData();
            if (curatedWork != null) {
                for (List<CuratedWork> workList : curatedWork.values()) {
                    for (CuratedWork work : workList) {
                        if (work.getProjectId() != null) {
                            Project project = projectService.getProject(work.getProjectId());
                            if (project != null && project.getSection() != null) {
                                for (Section section : project.getSection()) {
                                    if (section.getTasks() != null) {
                                        for (Task task : section.getTasks()) {
                                            workspaceService.addTaskToCalendar(calendar, task, project, "workspace", dateFormat, startDate, endDate);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            response.put("message", "Calendar tasks retrieved successfully");
            response.put("calendar", calendar);
            response.put("workspaceName", workspace.getName());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error retrieving workspace calendar tasks: ", e);
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An internal error occurred while retrieving calendar tasks");
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/add-knowledge-entry")
    public ResponseEntity<?> addKnowledgeEntry(HttpServletRequest request, @RequestBody Entry entry){

        String emailId = tokenService.getEmailFromToken(request);

        try {
            User user = userService.getUser(emailId);
            HashMap<String, Object> response = new HashMap<>();

            if (user != null) {

                if(entry != null) {

                    if(workspaceService.createKnowledgeEntry(user.getId(), entry)) {
                        response.put("message", "Entry added successfully");
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }else{
                        response.put("message", "Workspace not found");
                        log.error("Not Stored");
                    }

                }
                else{
                    response.put("message", "User not found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }

            }else{
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An internal error occurred.");
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    @GetMapping("/get-Knowledge-entry")
    public ResponseEntity<?> getKnowledgeEntry(HttpServletRequest request) {

        String emailId = tokenService.getEmailFromToken(request);

        try{
            User user = userService.getUser(emailId);
            HashMap<String, Object> response = new HashMap<>();

            HashMap<String,String> knowledgeEntry = workspaceService.getAllKnowledgeEntries(user.getId());

            if(knowledgeEntry != null) {
                    response.put("message", "Knowledge entry retrieved successfully");
                    response.put("knowledgeEntry", knowledgeEntry);
                    return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
            HashMap<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An internal error occurred.");
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
