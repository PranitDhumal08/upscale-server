package com.upscale.upscale.service.Workspace;


import com.upscale.upscale.dto.workspace.Entry;
import com.upscale.upscale.entity.workspace.Knowledge;
import com.upscale.upscale.entity.workspace.Workspace;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.KnowledgeRepo;
import com.upscale.upscale.repository.WorkspaceRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import com.upscale.upscale.service.project.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class WorkspaceService {

    @Autowired
    private WorkspaceRepo workspaceRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private PortfolioService portfolioService;

    public void save(Workspace workspace) {
        workspaceRepo.save(workspace);
    }

    public Workspace createWorkspace(String userId) {

        Workspace workspace = new Workspace();

        workspace.setName("My Workspace");
        workspace.setUserId(userId);

        List<String> members = new ArrayList<>();
        members.add(userId);
        workspace.setMembers(members);

        save(workspace);
        log.info("Created Workspace: " + workspace.getName() + " with ID: " + workspace.getUserId());


        Workspace workspace1 = workspaceRepo.findByUserId(userId);
        return workspace1;
    }

    public Workspace getWorkspace(String userId) {

        return workspaceRepo.findByUserId(userId);
    }

    public Workspace getWorkspaceByUserEmailId(String email) {
        User user = userService.getUser(email);
        return workspaceRepo.findByUserId(user.getId());
    }

    public boolean setCuratedWork(String sectionName, String userId, CuratedWork curatedWork) {
        Workspace workspace = getWorkspace(userId);

        HashMap<String,List<CuratedWork>> curatedWorkHashMap = workspace.getCuratedWorkData();

        // Only allow adding work to existing sections
        if (!curatedWorkHashMap.containsKey(sectionName)) {
            log.warn("Attempted to add work to non-existent section: {}", sectionName);
            return false;
        }

        List<CuratedWork> curatedWorkList = curatedWorkHashMap.get(sectionName);

        Project project = projectService.getProject(curatedWork.getProjectId());
        Optional<Portfolio> portfolio = portfolioService.getPortfolio(curatedWork.getProjectId());
        if(project != null){
            curatedWork.setProjectName(project.getProjectName());
        }
        else if(portfolio != null && portfolio.get() != null) {
            Portfolio portfolio1 = portfolio.get();
            curatedWork.setProjectName(portfolio1.getPortfolioName());
        }

        // Check if project already exists in this section
        boolean projectExists = false;
        for(CuratedWork curatedWorkData : curatedWorkList) {
            if(curatedWork.getProjectId().equals(curatedWorkData.getProjectId())) {
                projectExists = true;
                log.warn("Project {} already exists in section {}", curatedWork.getProjectId(), sectionName);
                break;
            }
        }

        // Allow adding even if it exists (just log a warning)
        curatedWorkList.add(curatedWork);
        curatedWorkHashMap.put(sectionName, curatedWorkList);
        workspace.setCuratedWorkData(curatedWorkHashMap);
        workspaceRepo.save(workspace);

        return true;
    }

    public HashMap<String,String> getMemberInfo(String userId){

        User user = userService.getUserById(userId);

        HashMap<String,String> userInfo = new HashMap<>();

        if(user != null){

            if(!user.getFullName().isEmpty())userInfo.put("Name",user.getFullName());
            else userInfo.put("Name",user.getEmailId());

            userInfo.put("jobTitle",user.getJobTitle());

            return userInfo;
        }

        return null;
    }

    public HashMap<String, Object> getAllwork(String userId) {
        Workspace workspace = getWorkspace(userId);

        HashMap<String, List<HashMap<String, String>>> projectsToMembersMap = new HashMap<>();

        if (workspace != null) {
            HashMap<String, List<CuratedWork>> curatedWorkHashMap = workspace.getCuratedWorkData();

            for (List<CuratedWork> curatedWorkList : curatedWorkHashMap.values()) {
                for (CuratedWork curatedWork : curatedWorkList) {
                    String projectName = curatedWork.getProjectName();
                    String projectId = curatedWork.getProjectId();


                    if (projectsToMembersMap.containsKey(projectName)) {
                        continue;
                    }

                    Project project = projectService.getProject(projectId);
                    Optional<Portfolio> portfolioOpt = portfolioService.getPortfolio(projectId);

                    List<String> teamMemberIds = new ArrayList<>();

                    if (portfolioOpt.isPresent()) {
                        teamMemberIds = portfolioOpt.get().getTeammates();
                    } else if (project != null) {
                        teamMemberIds = project.getTeammates();
                    }


                    List<HashMap<String, String>> teamMembersInfoList = new ArrayList<>();
                    if (teamMemberIds != null && !teamMemberIds.isEmpty()) {
                        for (String teamMemberId : teamMemberIds) {
                            User user = userService.getUserById(teamMemberId);
                            if (user != null) {
                                HashMap<String, String> teamMemberInfo = new HashMap<>();
                                teamMemberInfo.put("fullName", user.getFullName());
                                teamMemberInfo.put("emailId", user.getEmailId());
                                teamMemberInfo.put("jobTitle", user.getJobTitle());

                                //System.out.println(teamMemberInfo);
                                teamMembersInfoList.add(teamMemberInfo);
                            }
                        }
                    }
                    projectsToMembersMap.put(projectName, teamMembersInfoList);
                }
            }
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("members", projectsToMembersMap);
        return res;
    }

    public void addTaskToCalendar(
            HashMap<String, List<HashMap<String, Object>>> calendar,
            Task task,
            Project project,
            String source,
            SimpleDateFormat dateFormat,
            Date startDate,
            Date endDate
    ) {
        try {
            // Determine the task date (prefer startDate, fallback to date)
            Date taskDate = task.getStartDate() != null ? task.getStartDate() : task.getDate();
            if (taskDate == null) return;

            // Filter by date range if provided
            if (startDate != null && taskDate.before(startDate)) return;
            if (endDate != null && taskDate.after(endDate)) return;

            String dateKey = dateFormat.format(taskDate);

            // Build task information
            HashMap<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("id", task.getId());
            taskInfo.put("taskName", task.getTaskName());
            taskInfo.put("description", task.getDescription());
            taskInfo.put("priority", task.getPriority());
            taskInfo.put("status", task.getStatus());
            taskInfo.put("completed", task.isCompleted());
            taskInfo.put("startDate", task.getStartDate());
            taskInfo.put("endDate", task.getEndDate());
            taskInfo.put("date", task.getDate());
            taskInfo.put("source", source); // "project" or "portfolio"
            taskInfo.put("projectId", project.getId());
            taskInfo.put("projectName", project.getProjectName());

            // Get assignee names
            List<String> assigneeNames = new ArrayList<>();
            if (task.getAssignId() != null) {
                for (String userId : task.getAssignId()) {
                    try {
                        User assignee = userService.getUserById(userId);
                        String name = (assignee != null && assignee.getFullName() != null)
                                ? assignee.getFullName() : userId;
                        assigneeNames.add(name);
                    } catch (Exception e) {
                        log.warn("Could not find user for id: {}", userId);
                        assigneeNames.add(userId);
                    }
                }
            }
            taskInfo.put("assignees", assigneeNames);

            // Add to calendar
            calendar.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(taskInfo);

        } catch (Exception e) {
            log.error("Error adding task to calendar: ", e);
        }
    }

    @Autowired
    private KnowledgeRepo knowledgeRepo;

    public boolean createKnowledgeEntry(String userId, Entry entry) {
        try {
            Workspace workspace = getWorkspace(userId);
            log.info("Creating knowledge entry for user: {}, workspace: {}", userId, workspace != null ? workspace.getId() : "null");

            if(workspace == null || entry == null) {
                log.error("Workspace or entry is null. Workspace: {}, Entry: {}", workspace, entry);
                return false;
            }

            Knowledge knowledge = new Knowledge();
            knowledge.setWorkspaceId(workspace.getId());
            knowledge.setEntryName(entry.getEntryName());
            knowledge.setEntryDescription(entry.getEntryDescription());

            Knowledge savedKnowledge = knowledgeRepo.save(knowledge);
            log.info("Created knowledge entry with ID: {}", savedKnowledge.getId());

            // Use the saved knowledge entry directly
            LinkedList<String> knowlegeIds = new LinkedList<>(workspace.getKnowledgeId());
            knowlegeIds.addFirst(savedKnowledge.getId());
            workspace.setKnowledgeId(knowlegeIds);
            save(workspace);
            log.info("Updated workspace with new knowledge entry. Total entries: {}", knowlegeIds.size());
            return true;
        } catch (Exception e) {
            log.error("Error creating knowledge entry: ", e);
            return false;
        }
    }

    public List<HashMap<String, String>> getAllKnowledgeEntries(String userId) {
        Workspace workspace = getWorkspace(userId);
        if (workspace == null) return null;

        List<String> knowledgeIds = workspace.getKnowledgeId();
        List<HashMap<String, String>> knowledgeList = new ArrayList<>();
        for (String knowledgeId : knowledgeIds) {
            Optional<Knowledge> knowledge = knowledgeRepo.findById(knowledgeId);
            if (knowledge.isPresent()) {
                Knowledge knowledge1 = knowledge.get();
                log.info("Found knowledge entry: {}", knowledge1);
                HashMap<String, String> knowledgeMap = new HashMap<>();
                knowledgeMap.put("id", knowledge1.getId());
                knowledgeMap.put("entryName", knowledge1.getEntryName());
                knowledgeMap.put("entryDescription", knowledge1.getEntryDescription());
                knowledgeMap.put("projectId", knowledge1.getWorkspaceId());
                knowledgeMap.put("workspaceName", workspace.getName());
                knowledgeList.add(knowledgeMap);
            }
        }
        return knowledgeList;
    }

    public boolean deleteKnowledgeEntry(String userId, String knowledgeId) {
        Workspace workspace = getWorkspace(userId);

        if(workspace != null) {

            List<String> knowledgeIds = workspace.getKnowledgeId();

            if(!knowledgeIds.isEmpty()) {
                if(knowledgeIds.contains(knowledgeId)) {

                    Knowledge knowledge = knowledgeRepo.findById(knowledgeId).get();

                    if(knowledge != null) {
                        knowledgeRepo.delete(knowledge);

                        knowledgeIds.remove(knowledgeId);
                        log.info("Deleted knowledge entry with ID: {}", knowledgeId);
                        save(workspace);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
