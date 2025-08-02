package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.*;
import com.upscale.upscale.dto.task.TaskData;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.ProjectRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ProjectService {

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    @Lazy
    private TaskService taskService;

    @Autowired
    @Lazy
    private InboxService inboxService;

    @Autowired
    private SectionService sectionService;

    public void save(Project project){
        projectRepo.save(project);
    }
    public Project getProject(String projectId){
        return projectRepo.findById(projectId).orElse(null);
    }

    public Task setTask(TaskData taskData, String createdId, String email) {

        log.info("Received TaskData: {}", taskData);

        Task task = new Task();
        task.setTaskName(taskData.getTaskName());
        task.setStartDate(taskData.getStartDate() != null ? taskData.getStartDate() : taskData.getDate());
        task.setEndDate(taskData.getEndDate());
        task.setDate(taskData.getDate());
        task.setCompleted(false);
        task.setCreatedId(createdId);
        task.setProjectIds(taskData.getProjectIds());
        task.setDescription(taskData.getDescription());


        List<String> assignId = new ArrayList<>();
        for(String id:taskData.getAssignId()){

            if(id != createdId){
                inboxService.sendTaskDetails(task,email,id);
            }

            User user = userService.getUser(id);

            assignId.add(user.getId());
        }

        task.setAssignId(assignId);

        Task savedTask = taskService.save(task);
        log.info("Saved Task to DB: {}", savedTask);

        return savedTask;

    }

    public HashMap<String, List<String>> getTasks(String projectId, List<String> teammates, String creatorId, HashMap<String,List<String>> tasks){
        HashMap<String, List<String>> newTasks = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : tasks.entrySet()) {
            String groupName = entry.getKey();
            List<String> taskNames = entry.getValue();

            List<String> taskIds = new ArrayList<>();
            for (String taskName : taskNames) {
                Task task = new Task();
                task.setTaskName(taskName);
                task.setGroup(groupName);
                task.setCompleted(false);
                task.setDate(new Date());
                task.setProjectIds(Collections.singletonList(projectId)); // Set project ID
                task.setAssignId(teammates); // Assign to all teammates
                task.setCreatedId(creatorId); // Set the creator ID
                // Save the task to get an ID
                Task savedTask = taskService.save(task);
                taskIds.add(savedTask.getId());
            }

            newTasks.put(groupName, taskIds);
        }

        return newTasks;
    }

    public List<Section> getSections(String projectId, List<String> teammates, String creatorId, HashMap<String, List<String>> tasksMap) {
        List<Section> sections = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : tasksMap.entrySet()) {
            String sectionName = entry.getKey();
            List<String> taskNames = entry.getValue();

            Section section = new Section();
            section.setId(UUID.randomUUID().toString());
            section.setSectionName(sectionName);
            List<Task> taskList = new ArrayList<>();

            for (String taskName : taskNames) {
                Task task = new Task();
                task.setTaskName(taskName);
                task.setGroup(sectionName);
                task.setCompleted(false);
                task.setDate(new Date());
                task.setProjectIds(Collections.singletonList(projectId));
                task.setAssignId(teammates);
                task.setCreatedId(creatorId);

                Task savedTask = taskService.save(task);
                taskList.add(savedTask);
            }

            // Convert task objects to task IDs for storage
            List<String> taskIds = new ArrayList<>();
            for (Task task : taskList) {
                taskIds.add(task.getId());
            }
            section.setTaskIds(taskIds);
            //sectionService.save(section);
            sections.add(section);
        }

        return sections;
    }

    public boolean setProject(String emailId, ProjectCreate projectCreate) {
        if(emailId.isEmpty()) return false;

        Project newProject = new Project();

        newProject.setUserEmailid(emailId);
        newProject.setProjectName(projectCreate.getProjectName());
        newProject.setWorkspace(projectCreate.getWorkspace());

        // Save the project first to get an ID before setting tasks
        save(newProject);

        // Get the creator's ID
        String creatorId = userService.getUser(emailId).getId();

        // Now pass the project ID, teammates, and creator ID to getTasks
        List<Section> sections = getSections(newProject.getId(), projectCreate.getTeammates(), creatorId, projectCreate.getTasks());
        newProject.setSection(sections);

        newProject.setLayouts(projectCreate.getLayouts());
        newProject.setRecommended(projectCreate.getRecommended());
        newProject.setPopular(projectCreate.getPopular());
        newProject.setOther(projectCreate.getOther());

        // Initialize teammates HashMap with creator as owner
        HashMap<String, String[]> teammatesMap = new HashMap<>();
        User creator = userService.getUser(emailId);
        
        // Add creator as owner
        String[] creatorInfo = {
            creator.getRole() != null ? creator.getRole() : "Project Manager", // [0] Role in project
            "owner", // [1] Position
            emailId, // [2] Email
            creator.getFullName() // [3] Full name
        };
        teammatesMap.put(creator.getFullName(), creatorInfo);

        // Process teammates and send invitations
        if (projectCreate.getTeammates() != null) {
            for (String teammateEmail : projectCreate.getTeammates()) {
                // Check if teammate exists in database
                User teammateUser = userService.getUser(teammateEmail);
                if (teammateUser != null) {
                    String[] teammateInfo = {
                        teammateUser.getRole() != null ? teammateUser.getRole() : "Team Member", // [0] Role in project
                        "employee", // [1] Position
                        teammateEmail, // [2] Email
                        teammateUser.getFullName()  // [3] Full name
                    };
                    teammatesMap.put(teammateUser.getId(), teammateInfo);
                    
                    // Send project invitation via inbox
                    inboxService.sendProjectInvite(emailId, teammateEmail, newProject, teammateUser);
                    log.info("Invitation sent to: {}", teammateEmail);
                } else {
                    log.warn("Teammate not found in database: {}", teammateEmail);
                }
            }
        }
        
        newProject.setTeammates(teammatesMap);



        save(newProject); // Save again with updated tasks and teammates
        return userService.setProject(newProject, emailId);
    }


    public boolean updateProject(String emailId, ProjectCreate projectCreate){
        Project project = getProject(emailId);

        if(project != null){

            if(project.getLayouts().isEmpty()) project.setLayouts(projectCreate.getLayouts());
            if(project.getRecommended().isEmpty()) project.setRecommended(projectCreate.getRecommended());
            if(project.getPopular().isEmpty()) project.setPopular(projectCreate.getPopular());
            if(project.getOther().isEmpty()) project.setOther(projectCreate.getOther());
            
            // Handle teammates HashMap update
            if(project.getTeammates().isEmpty() && projectCreate.getTeammates() != null) {
                HashMap<String, String[]> teammatesMap = new HashMap<>();
                User creator = userService.getUser(emailId);
                
                // Add creator as owner
                String[] creatorInfo = {
                    creator.getRole() != null ? creator.getRole() : "Project Manager", // [0] Role
                    "owner", // [1] Position
                    creator.getEmailId(), // [2] Email
                    creator.getFullName() // [3] Full name
                };
                teammatesMap.put(creator.getId(), creatorInfo);
                
                // Add teammates from ProjectCreate
                for (String teammateEmail : projectCreate.getTeammates()) {
                    User teammateUser = userService.getUser(teammateEmail);
                    if (teammateUser != null) {
                        String[] teammateInfo = {
                            teammateUser.getRole() != null ? teammateUser.getRole() : "Team Member", // [0] Role
                            "employee", // [1] Position
                            teammateEmail, // [2] Email
                            teammateUser.getFullName() // [3] Full name
                        };
                        teammatesMap.put(teammateUser.getFullName(), teammateInfo);
                    }
                }
                project.setTeammates(teammatesMap);
            }

            save(project);
            return true;
        }

        return false;
    }

    public ProjectData getInfo(String emailId){
        Project project = getProject(emailId);
        ProjectData projectData = new ProjectData();
        projectData.setProjectName(project.getProjectName());
        projectData.setWorkspace(project.getWorkspace());
        
        // This method now needs to resolve task IDs to Task objects.
        // For now, I'll leave it empty as the main focus is the /list/{project-id} endpoint.
        // projectData.setTasks(...); 
        
        projectData.setLayouts(project.getLayouts());
        projectData.setRecommended(project.getRecommended());
        projectData.setPopular(project.getPopular());
        projectData.setOther(project.getOther());
        projectData.setTeammates(project.getTeammates());
        return projectData;
    }

    public String getProjectName(String projectId){
        return projectRepo.findById(projectId).get().getProjectName();
    }

    public HashMap<String, String> getProjectsAsTeammate(String emailId) {
        HashMap<String, String> teammateProjects = new HashMap<>();
        List<Project> allProjects = projectRepo.findAll();
        
        User user = userService.getUser(emailId);
        if (user == null) {
            log.warn("User not found for email: {}", emailId);
            return teammateProjects;
        }
        
        for (Project project : allProjects) {
            if (project.getTeammates() != null) {
                // Check if user's email exists in any of the teammate entries
                for (String[] teammateInfo : project.getTeammates().values()) {
                    if (teammateInfo.length > 2 && teammateInfo[2].equals(emailId)) {
                        teammateProjects.put(project.getId(), project.getProjectName());
                        break; // Found user in this project, no need to check other teammates
                    }
                }
            }
        }
        
        return teammateProjects;
    }

    public Task addTaskToProject(String projectId, String creatorEmail, AddTaskToProjectRequest addTaskRequest) {
        Project project = getProject(projectId);
        if (project == null) {
            log.error("Project not found with id: {}", projectId);
            return null;
        }

        User creator = userService.getUser(creatorEmail);
        if (creator == null) {
            log.error("Creator user not found with email: {}", creatorEmail);
            return null;
        }

        Task task = new Task();
        task.setTaskName(addTaskRequest.getTaskName());
        task.setDescription(addTaskRequest.getDescription());
        task.setStartDate(addTaskRequest.getStartDate() != null ? addTaskRequest.getStartDate() : addTaskRequest.getDate());
        task.setEndDate(addTaskRequest.getEndDate());
        task.setDate(addTaskRequest.getDate());
        task.setPriority(addTaskRequest.getPriority());
        task.setStatus(addTaskRequest.getStatus());
        task.setCompleted(false);
        task.setCreatedId(creator.getId());
        String group = addTaskRequest.getGroup();
        if (group == null || group.trim().isEmpty()) {
            group = "To do"; // Default group
        }
        task.setGroup(group);
        task.setProjectIds(Collections.singletonList(projectId));

        List<String> assignIds = new ArrayList<>();
        if (addTaskRequest.getAssignId() != null) {
            for (String assigneeEmail : addTaskRequest.getAssignId()) {
                User assignee = userService.getUser(assigneeEmail);
                if (assignee != null) {
                    assignIds.add(assignee.getId());
                    if (!assignee.getId().equals(creator.getId())) {
                        inboxService.sendTaskDetails(task, creatorEmail, assigneeEmail);
                    }
                } else {
                    log.warn("Assignee user not found for email: {}", assigneeEmail);
                }
            }
        }
        task.setAssignId(assignIds);

        Task savedTask = taskService.save(task);
        log.info("Saved new task with id: {}", savedTask.getId());

        //project.getTasks().computeIfAbsent(group, k -> new ArrayList<>()).add(savedTask.getId());

        save(project);
        log.info("Updated project {} with new task {}", projectId, savedTask.getId());

        return savedTask;
    }

    public boolean addProjectSection(String projectId, SectionData sectionData) {
        if (sectionData == null || sectionData.getSectionName() == null || sectionData.getSectionName().isBlank())
            return false;

        Project project = getProject(projectId);
        if (project == null) return false;

        // Optional: prevent duplicate section names
        for (Section existing : project.getSection()) {
            if (existing.getSectionName().equalsIgnoreCase(sectionData.getSectionName())) {
                return false; // Don't add duplicate section names
            }
        }

        Section section = new Section();
        section.setId(UUID.randomUUID().toString()); // Ensure unique ID
        section.setSectionName(sectionData.getSectionName());

        project.getSection().add(section);
        save(project);

        System.out.println("Section added: " + section.getId() + " → " + section.getSectionName());

        return true;
    }

    public List<Project> getProjects() {
        return projectRepo.findAll();
    }

    public Boolean deleteProject(String projectId) {
        Project project = getProject(projectId);
        if (project == null) {
            log.error("Project not found with id: {}", projectId);
            return false;
        }
        projectRepo.delete(project);
        log.info("Deleted project with id: {}", projectId);

        List<User> users = userService.getAllUsers();

        for (User user : users) {

            List<String> myProjectIds = user.getProjects();

            myProjectIds.removeIf(pId -> pId.equals(projectId));

            user.setProjects(myProjectIds);
            userService.save(user);
        }

        return true;
    }

    public boolean deleteSection(String sectionId) {
        List<Project> projectList = getProjects();

        if (projectList.isEmpty()) return false;

        for (Project project : projectList) {
            List<Section> sections = project.getSection();

            Iterator<Section> iterator = sections.iterator();
            while (iterator.hasNext()) {
                Section section = iterator.next();

                if (section.getId().equals(sectionId)) {
                    List<String> taskIds = section.getTaskIds();

                    if (taskIds != null && !taskIds.isEmpty()) {
                        for (String taskId : taskIds) {
                            taskService.deleteTask(taskId);
                        }
                        log.info("Section's Task Deleted with id: {}", sectionId);
                    }

                    iterator.remove();
                    log.info("Deleted Section with id: {}", sectionId);
                }
            }
            save(project);
        }
        return true;
    }

    public Map<String, Object> getDashboardStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        List<Task> tasks = taskService.getTasksByProjectId(projectId);
        Project project = getProject(projectId);
        if (project == null) {
            stats.put("error", "Project not found");
            return stats;
        }
        int totalTasks = tasks.size();
        int totalCompletedTasks = 0;
        int totalIncompleteTasks = 0;
        int totalOverdueTasks = 0;
        Map<String, Integer> incompleteTasksBySection = new HashMap<>();
        Map<String, Integer> tasksByCompletionStatus = new HashMap<>();
        Map<String, Integer> upcomingTasksByAssignee = new HashMap<>();
        Map<String, Integer> completedTasksByDate = new HashMap<>();
        Map<String, Integer> totalTasksByDate = new HashMap<>();
        Date now = new Date();
        for (Section section : project.getSection()) {
            int incompleteCount = 0;
            if (section.getTaskIds() != null) {
                for (String taskId : section.getTaskIds()) {
                    Task task = taskService.getTask(taskId);
                    if (task != null && !task.isCompleted()) {
                        incompleteCount++;
                    }
                }
            }
            incompleteTasksBySection.put(section.getSectionName(), incompleteCount);
        }
        for (Task task : tasks) {
            boolean completed = task.isCompleted();
            if (completed) totalCompletedTasks++;
            else totalIncompleteTasks++;
            // Overdue: incomplete and due date before now
            if (!completed && task.getStartDate() != null && task.getStartDate().before(now)) {
                totalOverdueTasks++;
            }
            // Completion status
            String statusKey = completed ? "Completed" : "Incomplete";
            tasksByCompletionStatus.put(statusKey, tasksByCompletionStatus.getOrDefault(statusKey, 0) + 1);
            // Upcoming by assignee (only for incomplete tasks)
            if (!completed && task.getAssignId() != null) {
                for (String userId : task.getAssignId()) {
                    upcomingTasksByAssignee.put(userId, upcomingTasksByAssignee.getOrDefault(userId, 0) + 1);
                }
            }
            // Completion over time (by date, formatted as yyyy-MM-dd)
            if (task.getStartDate() != null) {
                String dateKey = new java.text.SimpleDateFormat("yyyy-MM-dd").format(task.getStartDate());
                totalTasksByDate.put(dateKey, totalTasksByDate.getOrDefault(dateKey, 0) + 1);
                if (completed) {
                    completedTasksByDate.put(dateKey, completedTasksByDate.getOrDefault(dateKey, 0) + 1);
                }
            }
        }
        // Build completion over time list
        List<Map<String, Object>> taskCompletionOverTime = new ArrayList<>();
        for (String date : totalTasksByDate.keySet()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", date);
            entry.put("total", totalTasksByDate.get(date));
            entry.put("completed", completedTasksByDate.getOrDefault(date, 0));
            taskCompletionOverTime.add(entry);
        }
        stats.put("totalCompletedTasks", totalCompletedTasks);
        stats.put("totalIncompleteTasks", totalIncompleteTasks);
        stats.put("totalOverdueTasks", totalOverdueTasks);
        stats.put("totalTasks", totalTasks);
        stats.put("incompleteTasksBySection", incompleteTasksBySection);
        stats.put("tasksByCompletionStatus", tasksByCompletionStatus);
        stats.put("upcomingTasksByAssignee", upcomingTasksByAssignee);
        stats.put("taskCompletionOverTime", taskCompletionOverTime);
        return stats;
    }

    public boolean updateProject(ProjectOverview projectOverview, String projectId) {

        Project project = getProject(projectId);

        if(project == null){
            log.error("Project not found");
            return false;
        }

        if(projectOverview.getProjectName() != null && !projectOverview.getProjectName().isEmpty()) {
            project.setProjectName(projectOverview.getProjectName());
            log.info("Project name updated to: {}", projectOverview.getProjectName());
        }
        if(projectOverview.getProjectDescription() != null && !projectOverview.getProjectDescription().isEmpty()) {
            project.setProjectDescription(projectOverview.getProjectDescription());
        }
        if(projectOverview.getStartDate() != null) project.setStartDate(projectOverview.getStartDate());
        if(projectOverview.getEndDate() != null) project.setEndDate(projectOverview.getEndDate());

        save(project);
        log.info("Project updated");
        return true;
    }

    public HashMap<String,Object> getProjectOverview(String projectId) {
        Project project = getProject(projectId);

        if(project == null){
            log.error("Project not found");
            return null;
        }

        HashMap<String,Object> data = new HashMap<>();

        data.put("Project name", project.getProjectName());
        data.put("Project description", project.getProjectDescription());
        
        // Format teammates information for Project Roles
        HashMap<String, Object> projectRoles = new HashMap<>();
        if (project.getTeammates() != null && !project.getTeammates().isEmpty()) {
            for (Map.Entry<String, String[]> entry : project.getTeammates().entrySet()) {
                String memberName = entry.getKey();
                String[] memberInfo = entry.getValue();
                
                HashMap<String, String> memberDetails = new HashMap<>();
                
                if (memberInfo.length == 3) {
                    // Format: ['role', 'position', 'email'] - like 'Shivammmm': ['dev', 'employee', 'shivamthorat21@gmail.com']
                    memberDetails.put("email", memberInfo[0]);     // email
                    memberDetails.put("role", memberInfo[1]); // role
                    memberDetails.put("position", memberInfo[2]);    // possition
                    memberDetails.put("name", memberName);        // name from key
                } else if (memberInfo.length >= 4) {
                    // Format: ['email', 'role', 'position', 'name'] - like '688e450e7cc0a5763a323ece': ['shivamthorat98@gmail.com', 'dev', 'employee', 'Shivammmm']
                    memberDetails.put("email", memberInfo[0]);    // email
                    memberDetails.put("role", memberInfo[1]);     // role
                    memberDetails.put("position", memberInfo[2]); // position
                    memberDetails.put("name", memberInfo[3]);     // name
                } else {
                    // Fallback for incomplete data
                    memberDetails.put("role", "null");
                    memberDetails.put("position", "null");
                    memberDetails.put("email", "null");
                    memberDetails.put("name", memberName);
                    log.warn("Incomplete teammate data for: {}", memberName);
                }
                
                projectRoles.put(memberName, memberDetails);
            }
        } else {
            // Fallback: if teammates HashMap is empty, show project owner
            User projectOwner = userService.getUser(project.getUserEmailid());
            if (projectOwner != null) {
                HashMap<String, String> ownerDetails = new HashMap<>();
                ownerDetails.put("role", projectOwner.getRole() != null ? projectOwner.getRole() : "Project Manager");
                ownerDetails.put("position", "owner");
                ownerDetails.put("email", project.getUserEmailid());
                ownerDetails.put("name", projectOwner.getFullName());
                
                projectRoles.put(projectOwner.getFullName(), ownerDetails);
            } else {
                // Handle case where project owner user is not found - show as null
                HashMap<String, String> ownerDetails = new HashMap<>();
                ownerDetails.put("role", "null");
                ownerDetails.put("position", "owner");
                ownerDetails.put("email", project.getUserEmailid() != null ? project.getUserEmailid() : "null");
                ownerDetails.put("name", "null");
                
                projectRoles.put("null", ownerDetails);
                log.warn("Project owner user not found for email: {}", project.getUserEmailid());
            }
        }
        
        data.put("Project Roles", projectRoles);
        data.put("Project start date",project.getStartDate());
        data.put("Project end date",project.getEndDate());

        log.info("Project overview Retrieved");
        return data;

    }

    /**
     * Duplicates a project with all its sections and tasks
     * @param originalProject The project to duplicate
     * @param newOwnerEmail The email of the user who will own the duplicated project
     * @return The duplicated project
     */
    public Project duplicateProject(Project originalProject, String newOwnerEmail) {
        try {
            // Create new project with duplicated data
            Project duplicatedProject = new Project();
            
            // Copy basic project information
            duplicatedProject.setProjectName(originalProject.getProjectName() + " (Copy)");
            duplicatedProject.setProjectDescription(originalProject.getProjectDescription());
            duplicatedProject.setUserEmailid(newOwnerEmail);
            duplicatedProject.setWorkspace(originalProject.getWorkspace());
            duplicatedProject.setLayouts(originalProject.getLayouts());
            duplicatedProject.setStartDate(originalProject.getStartDate());
            duplicatedProject.setEndDate(originalProject.getEndDate());
            duplicatedProject.setPortfolioPriority(originalProject.getPortfolioPriority());
            
            // Copy lists (create new instances to avoid reference issues)
            duplicatedProject.setRecommended(new ArrayList<>(originalProject.getRecommended()));
            duplicatedProject.setPopular(new ArrayList<>(originalProject.getPopular()));
            duplicatedProject.setOther(new ArrayList<>(originalProject.getOther()));
            
            // Copy teammates HashMap (create new HashMap with cloned arrays)
            HashMap<String, String[]> duplicatedTeammates = new HashMap<>();
            if (originalProject.getTeammates() != null) {
                for (Map.Entry<String, String[]> entry : originalProject.getTeammates().entrySet()) {
                    String[] originalInfo = entry.getValue();
                    String[] duplicatedInfo = originalInfo.clone(); // Clone the array
                    duplicatedTeammates.put(entry.getKey(), duplicatedInfo);
                }
            }
            duplicatedProject.setTeammates(duplicatedTeammates);
            
            // Initialize sections list
            duplicatedProject.setSection(new ArrayList<>());
            
            // Save the project first to get an ID
            save(duplicatedProject);
            log.info("Created duplicate project with ID: {}", duplicatedProject.getId());
            
            // Duplicate all sections and tasks
            if (originalProject.getSection() != null) {
                for (Section originalSection : originalProject.getSection()) {
                    Section duplicatedSection = duplicateSection(originalSection, duplicatedProject.getId());
                    duplicatedProject.getSection().add(duplicatedSection);
                }
            }
            
            // Save the project again with all sections
            save(duplicatedProject);
            
            log.info("Successfully duplicated project {} to {}", originalProject.getId(), duplicatedProject.getId());
            return duplicatedProject;
            
        } catch (Exception e) {
            log.error("Error duplicating project: ", e);
            return null;
        }
    }
    
    /**
     * Duplicates a section with all its tasks
     * @param originalSection The section to duplicate
     * @param newProjectId The ID of the new project
     * @return The duplicated section
     */
    private Section duplicateSection(Section originalSection, String newProjectId) {
        Section duplicatedSection = new Section();
        duplicatedSection.setId(java.util.UUID.randomUUID().toString());
        duplicatedSection.setSectionName(originalSection.getSectionName());
        duplicatedSection.setTaskIds(new ArrayList<>());
        
        // Duplicate all tasks in this section
        if (originalSection.getTaskIds() != null) {
            for (String originalTaskId : originalSection.getTaskIds()) {
                Task originalTask = taskService.getTask(originalTaskId);
                if (originalTask != null) {
                    Task duplicatedTask = duplicateTask(originalTask, newProjectId);
                    if (duplicatedTask != null) {
                        duplicatedSection.getTaskIds().add(duplicatedTask.getId());
                    }
                }
            }
        }
        
        log.info("Duplicated section '{}' with {} tasks", originalSection.getSectionName(), 
                duplicatedSection.getTaskIds().size());
        return duplicatedSection;
    }
    
    /**
     * Duplicates a task
     * @param originalTask The task to duplicate
     * @param newProjectId The ID of the new project
     * @return The duplicated task
     */
    private Task duplicateTask(Task originalTask, String newProjectId) {
        try {
            Task duplicatedTask = new Task();
            
            // Copy task data
            duplicatedTask.setTaskName(originalTask.getTaskName());
            duplicatedTask.setDescription(originalTask.getDescription());
            duplicatedTask.setPriority(originalTask.getPriority());
            duplicatedTask.setStatus(originalTask.getStatus());
            duplicatedTask.setGroup(originalTask.getGroup());
            duplicatedTask.setDate(originalTask.getDate());
            duplicatedTask.setStartDate(originalTask.getStartDate());
            duplicatedTask.setEndDate(originalTask.getEndDate());
            
            // Set as not completed (fresh start for duplicated tasks)
            duplicatedTask.setCompleted(false);
            
            // Copy assignees (keep the same people assigned)
            duplicatedTask.setAssignId(new ArrayList<>(originalTask.getAssignId()));
            
            // Set the new project ID
            duplicatedTask.setProjectIds(new ArrayList<>());
            duplicatedTask.getProjectIds().add(newProjectId);
            
            // Set creator as the person duplicating the project
            // Note: We don't have the creator ID here, so we'll leave it as original
            duplicatedTask.setCreatedId(originalTask.getCreatedId());
            
            // Save the duplicated task
            Task savedTask = taskService.save(duplicatedTask);
            log.debug("Duplicated task '{}' with ID: {}", originalTask.getTaskName(), savedTask.getId());
            
            return savedTask;
            
        } catch (Exception e) {
            log.error("Error duplicating task {}: ", originalTask.getId(), e);
            return null;
        }
    }
}
