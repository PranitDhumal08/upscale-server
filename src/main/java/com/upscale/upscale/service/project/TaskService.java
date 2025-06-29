package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.task.TaskData;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.TaskRepo;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
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
    private UserService userService;

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
        for(String assigneeEmail : taskData.getAssignId()){
            log.info("Processing assignee email: {}", assigneeEmail);
            // Get user by email ID
            User assigneeUser = userService.getUser(assigneeEmail);
            
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
        log.info("Final task before saving: {}", task);

        Task savedTask = save(task);
        log.info("Saved Task to DB: {}", savedTask);

        if(!taskData.getSectionId().isEmpty()){
            List<String> projectIds = taskData.getProjectIds();
            for(String projectId : projectIds){

                Project project = projectService.getProject(projectId);
                for (Section s : project.getSection()) {
                    if (s.getId() != null && s.getId().equals(taskData.getSectionId())) {
                        s.getTasks().add(task);
                    }
                }

                projectService.save(project); // only project, not section
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
        User user = userService.getUser(email);
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
        User user = userService.getUser(email);
        log.info("User ID: " + user.getId());

        //List<Task> assignedTasks = getTasksByAssignId(user.getId());

        List<Task> createdTasks = taskRepo.findByCreatedId(user.getId());

        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(createdTasks);
        //allTasks.addAll(assignedTasks);

        TaskData[] taskData = new TaskData[allTasks.size()];

        for (int i = 0; i < allTasks.size(); i++) {
            Task task = allTasks.get(i);
            taskData[i] = new TaskData();

            taskData[i].setId(task.getId());
            taskData[i].setTaskName(task.getTaskName());
            taskData[i].setCompleted(task.isCompleted());
            taskData[i].setDate(task.getDate());
            taskData[i].setStartDate(task.getStartDate());
            taskData[i].setEndDate(task.getEndDate());
            taskData[i].setDescription(task.getDescription());
            taskData[i].setAssignId(task.getAssignId());
            taskData[i].setProjectIds(task.getProjectIds());
            taskData[i].setPriority(task.getPriority());
            taskData[i].setStatus(task.getStatus());
        }

        return taskData;
    }

   public Task getTask(String id) {
        return taskRepo.findById(id).orElse(null);
   }

   public List<Task> getTasksByProjectId(String projectId) {
        return taskRepo.findByProjectIdsContaining(projectId);
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
                    List<Task> tasks = s.getTasks();
                    tasks.removeIf(t -> t.getId().equals(id));
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
        List<Project> projects = projectService.getProjects();

        for (Project project : projects) {
            List<Section> sections = project.getSection();
            for (Section section : sections) {
                List<Task> tasks = section.getTasks();
                for (Task task : tasks) {
                    if (task.getId().equals(taskId)) {
                        task.setCompleted(true);
                        projectService.save(project);
                        log.info("Updated Task: {}", task);
                        return;
                    }
                }
            }
        }

        log.warn("Task with ID {} not found.", taskId);
    }

    public boolean addTaskToProject(String taskName, String sectionId) {
        if(taskName == null || sectionId == null) return false;
        List<Project> projects = projectService.getProjects();
        for(Project project : projects){
            List<Section> sections = project.getSection();
            for(Section section : sections){
                if(sectionId.equals(section.getId())){
                    List<Task> tasks = section.getTasks();
                    Task newTask = new Task();
                    newTask.setTaskName(taskName);
                    save(newTask);
                    tasks.add(newTask);
                }
            }
            projectService.save(project);
        }
        return true;
    }

    public boolean updateTaskToProject(String taskId, TaskData taskData) {

        if(taskId == null || taskData == null) return false;

        Project project = projectService.getProject(taskData.getProjectIds().get(0));

        List<Section> sections = project.getSection();
        for(Section section : sections){
            List<Task> tasks = section.getTasks();
            for(Task task : tasks){
                if(taskId.equals(task.getId())){

                    if (taskData.getAssignId() != null) task.setAssignId(taskData.getAssignId());
                    if (taskData.getStartDate() != null) task.setStartDate(taskData.getStartDate());
                    if (taskData.getEndDate() != null) task.setEndDate(taskData.getEndDate());
                    if (taskData.getPriority() != null) task.setPriority(taskData.getPriority());
                    if (taskData.getStatus() != null) task.setStatus(taskData.getStatus());
                    if (taskData.getDescription() != null) task.setDescription(taskData.getDescription());

                    taskRepo.save(task);

                }
            }
        }
        projectService.save(project);
        return true;

    }
}
