package com.upscale.upscale.service;

import com.upscale.upscale.dto.TaskData;
import com.upscale.upscale.entity.Section;
import com.upscale.upscale.entity.Task;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.repository.TaskRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public Task save(Task task) {
        return taskRepo.save(task);
    }

    public boolean setTask(TaskData taskData,String createdId,String email) {

        log.info("Received TaskData: {}", taskData);
        log.info("CreatedId: {}, Email: {}", createdId, email);

        Task task = new Task();
        task.setTaskName(taskData.getTaskName());
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

        if(!taskData.getSectionId().isEmpty()){
            Optional<Section> section = sectionService.findById(taskData.getSectionId());

            if(section.isPresent()){
                section.get().getTasks().add(task);

                sectionService.save(section.get());
            }
        }

        task.setAssignId(assignId);
        log.info("Final task before saving: {}", task);

        Task savedTask = save(task);
        log.info("Saved Task to DB: {}", savedTask);

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

}
