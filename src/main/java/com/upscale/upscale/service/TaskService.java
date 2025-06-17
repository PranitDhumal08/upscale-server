package com.upscale.upscale.service;

import com.upscale.upscale.dto.TaskData;
import com.upscale.upscale.entity.Task;
import com.upscale.upscale.entity.User;
import com.upscale.upscale.repository.TaskRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Task save(Task task) {
        return taskRepo.save(task);
    }

    public boolean setTask(TaskData taskData,String createdId,String email) {

        log.info("Received TaskData: {}", taskData);

        Task task = new Task();
        task.setTaskName(taskData.getTaskName());
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
        }

        return taskData;
    }

   public Task getTask(String id) {
        return taskRepo.findById(id).orElse(null);
   }

   public Task createTask(String taskName) {
        Task task = new Task();
        task.setTaskName(taskName);
        task.setCompleted(false);

        save(task);
        return task;
   }

}
