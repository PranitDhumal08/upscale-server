package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.task.TaskData;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.TaskRepo;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserLookupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TaskService {

    @Autowired
    private TaskRepo taskRepo;

    @Autowired
    private UserLookupService userLookupService;

    @Autowired
    private InboxService inboxService;
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    @Lazy
    private ProjectService projectService;

    public Task save(Task task) {
        return taskRepo.save(task);
    }

    public boolean setTask(TaskData taskData,String createdId,String email) {


        log.info("Received TaskData: {}", taskData);
        log.info("CreatedId: {}, Email: {}", createdId, email);

        Task task = new Task();
        task.setTaskName(taskData.getTaskName());
        task.setStartDate(taskData.getStartDate() != null ? taskData.getStartDate() : taskData.getDate());
        task.setEndDate(taskData.getEndDate());
        task.setDate(taskData.getDate());
        task.setCompleted(false);
        task.setPriority(taskData.getPriority());
        task.setStatus(taskData.getStatus());
        task.setCreatedId(createdId);
        task.setProjectIds(taskData.getProjectIds());
        task.setDescription(taskData.getDescription());

        log.info("Task object created: {}", task);

        List<String> assignId = new ArrayList<>();
        
        // If no assignees provided or assignId list is empty, assign to creator
        if(taskData.getAssignId() == null || taskData.getAssignId().isEmpty()) {
            log.info("No assignees provided, assigning task to creator: {}", email);
            assignId.add(createdId);
            task.setAssignId(assignId);
        } else {
            // Process provided assignees
            for(String assigneeEmail : taskData.getAssignId()){
                log.info("Processing assignee email: {}", assigneeEmail);
                // Get user by email ID
                User assigneeUser = userLookupService.getUserByEmail(assigneeEmail);

                if(assigneeUser != null) {
                    String assigneeUserId = assigneeUser.getId();
                    log.info("Found user for email {}: {}", assigneeEmail, assigneeUserId);

                    // Send inbox notification if assignee is different from creator
                    if(!assigneeUserId.equals(createdId)){
                        log.info("Sending task details to: {}", assigneeEmail);
                        inboxService.sendTaskDetails(task, email, assigneeEmail);
                    }

                    assignId.add(assigneeUserId);
                } else {
                    log.warn("User not found for email: {}", assigneeEmail);
                }
            }
            task.setAssignId(assignId);
        }
        
        log.info("Final task before saving: {}", task);


        Task savedTask = save(task);
        log.info("Saved Task to DB: {}", savedTask);

        // Add task to project sections if projectIds are provided
        if(taskData.getProjectIds() != null && !taskData.getProjectIds().isEmpty()){
            List<String> projectIds = taskData.getProjectIds();
            for(String projectId : projectIds){
                Project project = projectService.getProject(projectId);
                if(project != null) {
                    log.info("Adding task to project: {}", projectId);
                    
                    // If sectionId is provided, add to specific section
                    if(taskData.getSectionId() != null && !taskData.getSectionId().isEmpty()){
                        boolean sectionFound = false;
                        for (Section s : project.getSection()) {
                            if (s.getId() != null && s.getId().equals(taskData.getSectionId())) {
                                s.getTaskIds().add(savedTask.getId());
                                sectionFound = true;
                                log.info("Task added to section: {}", taskData.getSectionId());
                                break;
                            }
                        }
                        if(!sectionFound) {
                            log.warn("Section with ID {} not found in project {}", taskData.getSectionId(), projectId);
                        }
                    } else {
                        // If no sectionId provided, add to first available section or create a default one
                        if(project.getSection() != null && !project.getSection().isEmpty()) {
                            // Add to first section if no specific section is mentioned
                            project.getSection().get(0).getTaskIds().add(savedTask.getId());
                            log.info("Task added to first section of project: {}", projectId);
                        } else {
                            log.warn("No sections found in project: {}", projectId);
                        }
                    }
                    
                    projectService.save(project);
                } else {
                    log.warn("Project with ID {} not found", projectId);
                }
            }
        }

        return savedTask != null;
    }



    public List<Task> getTasksByAssignId(String assignId) {
        List<Task> tasks = new ArrayList<>();

        List<Task> allTasks = taskRepo.findAll();
        for(Task task:allTasks){
            if(task.getAssignId().contains(assignId)){
                tasks.add(task);
            }
        }
        return tasks;
    }

    public TaskData[] getTaskDataByAssignId(String email) {
        User user = userLookupService.getUserByEmail(email);
        log.info("User ID: " + user.getId());

        List<Task> assignedTasks = getTasksByAssignId(user.getId());

        TaskData[] taskData = new TaskData[assignedTasks.size()];

        for(int i = 0; i < assignedTasks.size(); i++){
            taskData[i] = new TaskData();
            taskData[i].setTaskName(assignedTasks.get(i).getTaskName());
            taskData[i].setDate(assignedTasks.get(i).getDate());
            taskData[i].setStartDate(assignedTasks.get(i).getStartDate());
            taskData[i].setEndDate(assignedTasks.get(i).getEndDate());
            taskData[i].setCompleted(assignedTasks.get(i).isCompleted());
            taskData[i].setAssignId(assignedTasks.get(i).getAssignId());
            taskData[i].setDescription(assignedTasks.get(i).getDescription());
            taskData[i].setPriority(assignedTasks.get(i).getPriority());
            taskData[i].setStatus(assignedTasks.get(i).getStatus());

        }
        return taskData;
    }

    public TaskData[] getAll(String email) {
        User user = userLookupService.getUserByEmail(email);
        log.info("User ID: " + user.getId());

        // Get tasks assigned to me (including self-assigned tasks)
        List<Task> assignedTasks = getTasksByAssignId(user.getId());
        
        // Get tasks created by me
        List<Task> createdTasks = taskRepo.findByCreatedId(user.getId());

        // Combine both lists, avoiding duplicates
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(assignedTasks);
        
        // Add created tasks that are not already in assignedTasks
        for(Task createdTask : createdTasks) {
            boolean alreadyExists = false;
            for(Task assignedTask : assignedTasks) {
                if(assignedTask.getId().equals(createdTask.getId())) {
                    alreadyExists = true;
                    break;
                }
            }
            if(!alreadyExists) {
                allTasks.add(createdTask);
            }
        }

        List<TaskData> taskDataList = new ArrayList<>();

        for (Task task : allTasks) {
            TaskData taskData = new TaskData();
            taskData.setId(task.getId());
            taskData.setTaskName(task.getTaskName());
            taskData.setCompleted(task.isCompleted());
            taskData.setDate(task.getDate());
            taskData.setStartDate(task.getStartDate());
            taskData.setEndDate(task.getEndDate());
            taskData.setDescription(task.getDescription());
            taskData.setAssignId(task.getAssignId());
            taskData.setProjectIds(task.getProjectIds());
            taskData.setPriority(task.getPriority());
            taskData.setStatus(task.getStatus());
            taskDataList.add(taskData);
        }

        return taskDataList.toArray(new TaskData[0]);
    }

    public TaskData[] getAssign(String email) {
        User user = userLookupService.getUserByEmail(email);
        log.info("User ID: " + user.getId());

        // Get tasks created by me
        List<Task> createdTasks = taskRepo.findByCreatedId(user.getId());

        List<TaskData> taskDataList = new ArrayList<>();

        for (Task task : createdTasks) {
            // Only include tasks that I assigned to OTHER users (not to myself)
            // This means the task should have assignees, but I should not be the only assignee
            if(task.getAssignId() != null && !task.getAssignId().isEmpty()) {
                // Check if there are other assignees besides me, or if I'm not assigned at all
                boolean hasOtherAssignees = false;
                for(String assigneeId : task.getAssignId()) {
                    if(!assigneeId.equals(user.getId())) {
                        hasOtherAssignees = true;
                        break;
                    }
                }
                
                // Include this task if it has other assignees (meaning I assigned it to someone else)
                if(hasOtherAssignees) {
                    TaskData taskData = new TaskData();
                    taskData.setId(task.getId());
                    taskData.setTaskName(task.getTaskName());
                    taskData.setCompleted(task.isCompleted());
                    taskData.setDate(task.getDate());
                    taskData.setStartDate(task.getStartDate());
                    taskData.setEndDate(task.getEndDate());
                    taskData.setDescription(task.getDescription());
                    taskData.setAssignId(task.getAssignId());
                    taskData.setProjectIds(task.getProjectIds());
                    taskData.setPriority(task.getPriority());
                    taskData.setStatus(task.getStatus());
                    taskDataList.add(taskData);
                }
            }
        }

        return taskDataList.toArray(new TaskData[0]);
    }

   public Task getTask(String id) {
        return taskRepo.findById(id).orElse(null);
   }

   public List<Task> getTasksByProjectId(String projectId) {
        return taskRepo.findByProjectIdsContaining(projectId);
   }

   /**
    * Helper method to get Task objects from a list of task IDs
    */
   public List<Task> getTasksByIds(List<String> taskIds) {
        List<Task> tasks = new ArrayList<>();
        if (taskIds != null) {
            for (String taskId : taskIds) {
                Task task = getTask(taskId);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }
        return tasks;
   }

   public Task createTask(String taskName) {
        Task task = new Task();
        task.setTaskName(taskName);
        task.setCompleted(false);

        save(task);
        return task;
   }

   public boolean deleteTask(String id) {
        Task task = getTask(id);
        if(task != null){

            List<Project> projectList = projectService.getProjects();
            for(Project project : projectList){

                List<Section> section = project.getSection();
                for(Section s : section){
                    List<String> taskIds = s.getTaskIds();
                    taskIds.removeIf(taskId -> taskId.equals(id));
                }
                projectService.save(project);

            }
            taskRepo.delete(task);
            log.info("Deleted Task: {}", task);
            return true;
        }
        return false;
   }

    public void updateTask(String taskId) {
        // Update the task directly in the database
        Task task = getTask(taskId);
        if(task != null) {
            task.setCompleted(true);
            save(task);
            log.info("Updated Task: {}", task);
        } else {
            log.warn("Task with ID {} not found.", taskId);
        }
    }

    public boolean addTaskToProject(String taskName, String sectionId) {
        if(taskName == null || sectionId == null) return false;
        List<Project> projects = projectService.getProjects();
        for(Project project : projects){
            List<Section> sections = project.getSection();
            for(Section section : sections){
                if(sectionId.equals(section.getId())){
                    Task newTask = new Task();
                    newTask.setTaskName(taskName);
                    Task savedTask = save(newTask);
                    section.getTaskIds().add(savedTask.getId());
                }
            }
            projectService.save(project);
        }
        return true;
    }

    public boolean updateTaskToProject(String taskId, TaskData taskData) {

        if(taskId == null || taskData == null) return false;

        // Get the task directly from the database and update it
        Task task = getTask(taskId);
        if(task != null) {
            if (taskData.getAssignId() != null) task.setAssignId(taskData.getAssignId());
            if (taskData.getStartDate() != null) task.setStartDate(taskData.getStartDate());
            if (taskData.getEndDate() != null) task.setEndDate(taskData.getEndDate());
            if (taskData.getPriority() != null) task.setPriority(taskData.getPriority());
            if (taskData.getStatus() != null) task.setStatus(taskData.getStatus());
            if (taskData.getDescription() != null) task.setDescription(taskData.getDescription());

            taskRepo.save(task);
            return true;
        }
        
        return false;
    }
}
