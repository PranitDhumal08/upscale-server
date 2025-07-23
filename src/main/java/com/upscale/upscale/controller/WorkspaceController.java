package com.upscale.upscale.controller;

import com.upscale.upscale.entity.Workspace;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.Workspace.CuratedWork;
import com.upscale.upscale.service.Workspace.WorkspaceService;
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
                    String[] member = new String[2];
                    User userMember = userService.getUserById(workspace.getMembers().get(i));

                    member[0] = userMember.getFullName();
                    member[1] = userMember.getEmailId();

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
}
