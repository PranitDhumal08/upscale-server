package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.task.TaskData;
import com.upscale.upscale.dto.task.UpdateScheduleRequest;
import com.upscale.upscale.dto.task.UpdateTaskRequest;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.project.SubTask;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.TaskRepo;
import com.upscale.upscale.repository.SubTaskRepo;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserLookupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Date;

@Service
@Slf4j
public class TaskService {

    @Autowired
    private TaskRepo taskRepo;

    @Autowired
    private UserLookupService userLookupService;

    @Autowired
    @Lazy
    private InboxService inboxService;
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SectionService sectionService;

    @Autowired
    @Lazy
    private ProjectService projectService;

    @Autowired
    private SubTaskRepo subTaskRepo;

    public Task save(Task task) {
        // Ensure ID is generated if null (backup solution)
        if (task.getId() == null) {
            log.info("Task ID is null before saving, MongoDB will auto-generate it");
        }
        Task savedTask = taskRepo.save(task);
        
        // Verify ID was generated
        if (savedTask.getId() == null) {
            log.error("CRITICAL: Task ID is still null after MongoDB save operation!");
            throw new RuntimeException("Failed to generate Task ID - MongoDB configuration issue");
        }
        
        log.info("Task successfully saved with ID: {}", savedTask.getId());
        return savedTask;
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
        log.info("Generated Task ID: {}", savedTask.getId());
        
        // Verify the task was saved with an ID
        if (savedTask.getId() == null) {
            log.error("ERROR: Task ID is null after saving! This indicates a MongoDB configuration issue.");
            return false;
        }

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
            taskData[i].setId(assignedTasks.get(i).getId()); // Add missing ID
            taskData[i].setTaskName(assignedTasks.get(i).getTaskName());
            taskData[i].setDate(assignedTasks.get(i).getDate());
            taskData[i].setStartDate(assignedTasks.get(i).getStartDate());
            taskData[i].setEndDate(assignedTasks.get(i).getEndDate());
            taskData[i].setCompleted(assignedTasks.get(i).isCompleted());
            taskData[i].setAssignId(assignedTasks.get(i).getAssignId());
            taskData[i].setDescription(assignedTasks.get(i).getDescription());
            taskData[i].setPriority(assignedTasks.get(i).getPriority());
            taskData[i].setStatus(assignedTasks.get(i).getStatus());
            taskData[i].setProjectIds(assignedTasks.get(i).getProjectIds()); // Add missing projectIds

        }
        return taskData;
    }

    public TaskData[] getAll(String email) {
        User user = userLookupService.getUserByEmail(email);
        log.info("User ID: " + user.getId());
        log.info("Getting all tasks for user: {}", email);

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
            log.info("Processing task with ID: {} and name: {}", task.getId(), task.getTaskName());
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
            log.info("Added TaskData with ID: {}", taskData.getId());
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

   public List<Task> getTasksByCreatedId(String createdId) {
        return taskRepo.findByCreatedId(createdId);
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

            // If task is set to repeat periodically, create the next instance N days after completion
            try {
                if ("PERIODICALLY".equalsIgnoreCase(task.getRepeatFrequency()) && task.getPeriodicDaysAfterCompletion() != null) {
                    int n = Math.max(0, task.getPeriodicDaysAfterCompletion());
                    Date completion = new Date();
                    long duration = 0L;
                    if (task.getStartDate() != null && task.getEndDate() != null) {
                        duration = Math.max(0L, task.getEndDate().getTime() - task.getStartDate().getTime());
                    }
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(completion);
                    cal.add(Calendar.DATE, n);
                    Date nextStart = cal.getTime();

                    Task instance = cloneAsInstance(task, nextStart, duration);
                    Task saved = save(instance);
                    addTaskInstanceToSameSections(task.getId(), saved.getId());
                    // Default: clone subtasks as part of the next instance
                    cloneSubTasksForInstance(task, saved, nextStart, new Date(nextStart.getTime() + duration));
                }
            } catch (Exception ignored) {}
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

    /**
     * Update task fields via UpdateTaskRequest alias used by PUT /api/task/update/{task-id}
     * - assign: list of assignee email IDs -> resolved to user IDs
     * - startDate, endDate
     * - priority, status
     */
    public boolean updateTaskFields(String taskId, UpdateTaskRequest req, String requesterEmail) {
        if (taskId == null || req == null) return false;

        Task task = getTask(taskId);
        if (task == null) return false;

        // Resolve assignee emails to user IDs (if provided)
        if (req.getAssign() != null) {
            List<String> resolvedIds = new ArrayList<>();
            for (String email : req.getAssign()) {
                try {
                    User u = userLookupService.getUserByEmail(email);
                    if (u != null) resolvedIds.add(u.getId());
                } catch (Exception ignored) {}
            }
            task.setAssignId(resolvedIds);
        }

        // Update scheduling fields if provided
        if (req.getStartDate() != null) task.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) task.setEndDate(req.getEndDate());
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        if (req.getStatus() != null) task.setStatus(req.getStatus());

        taskRepo.save(task);
        return true;
    }

    public boolean updateSchedule(String taskId, UpdateScheduleRequest req, String email) {
        Task template = getTask(taskId);
        if (template == null) return false;

        // Update base dates
        if (req.getStartDate() != null) template.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) template.setEndDate(req.getEndDate());

        // Recurrence settings on template
        String freq = req.getRepeatFrequency() == null ? "NONE" : req.getRepeatFrequency();
        template.setRepeatFrequency(freq);
        template.setRepeatDaysOfWeek(req.getDaysOfWeek() != null ? req.getDaysOfWeek() : new ArrayList<>());
        template.setMonthlyMode(req.getMonthlyMode());
        template.setMonthlyNth(req.getMonthlyNth());
        template.setMonthlyWeekday(req.getMonthlyWeekday());
        template.setMonthlyDayOfMonth(req.getMonthlyDayOfMonth());
        // periodic setting (only used when freq == PERIODICALLY)
        template.setPeriodicDaysAfterCompletion(null);
        template.setRecurrenceInstance(false);
        template.setRecurrenceParentId(null);

        save(template);

        if ("NONE".equalsIgnoreCase(freq)) return true;
        if ("PERIODICALLY".equalsIgnoreCase(freq)) {
            // Store periodic config and return. No pre-generation here.
            template.setMonthlyMode(null);
            template.setMonthlyNth(null);
            template.setMonthlyWeekday(null);
            template.setMonthlyDayOfMonth(null);
            template.setRepeatDaysOfWeek(new ArrayList<>());
            template.setPeriodicDaysAfterCompletion(req.getPeriodicDaysAfterCompletion());
            save(template);
            return true;
        }

        Date start = template.getStartDate();
        Date endBoundary = template.getEndDate();
        if (start == null || endBoundary == null) return true; // no occurrences without bounds

        long duration = 0L;
        if (template.getEndDate() != null && template.getStartDate() != null) {
            duration = template.getEndDate().getTime() - template.getStartDate().getTime();
        }

        List<Date> occurrences = new ArrayList<>();
        if ("WEEKLY".equalsIgnoreCase(freq)) {
            occurrences.addAll(generateWeeklyOccurrences(start, endBoundary, template.getRepeatDaysOfWeek()));
        } else if ("MONTHLY".equalsIgnoreCase(freq)) {
            if ("ON_NTH_WEEKDAY".equalsIgnoreCase(template.getMonthlyMode())) {
                occurrences.addAll(generateMonthlyNthWeekdayOccurrences(start, endBoundary, template.getMonthlyNth(), template.getMonthlyWeekday()));
            } else if ("ON_DAY_OF_MONTH".equalsIgnoreCase(template.getMonthlyMode())) {
                occurrences.addAll(generateMonthlyDayOccurrences(start, endBoundary, template.getMonthlyDayOfMonth()));
            }
        }

        // Avoid duplicating the template's own start date
        for (Date occStart : occurrences) {
            if (sameDay(occStart, start)) continue; // template represents the first
            Task instance = cloneAsInstance(template, occStart, duration);
            Task saved = save(instance);
            // place in same sections as template
            addTaskInstanceToSameSections(template.getId(), saved.getId());
            // clone subtasks if requested (default true)
            boolean cloneSubs = req.getCloneSubTasks() == null || req.getCloneSubTasks();
            if (cloneSubs) {
                cloneSubTasksForInstance(template, saved, occStart, new Date(occStart.getTime() + duration));
            }
        }

        return true;
    }

    private Task cloneAsInstance(Task template, Date occStart, long duration) {
        Task t = new Task();
        t.setTaskName(template.getTaskName());
        t.setCreatedId(template.getCreatedId());
        t.setProjectIds(new ArrayList<>(template.getProjectIds()));
        t.setCompleted(false);
        t.setPriority(template.getPriority());
        t.setStatus(template.getStatus());
        t.setGroup(template.getGroup());
        t.setDate(occStart);
        t.setDescription(template.getDescription());
        t.setAssignId(new ArrayList<>(template.getAssignId()));
        t.setStartDate(occStart);
        t.setEndDate(new Date(occStart.getTime() + Math.max(0, duration)));
        t.setSubTaskIds(new ArrayList<>());
        t.setRepeatFrequency(null); // instances are not generators
        t.setRepeatDaysOfWeek(new ArrayList<>());
        t.setMonthlyMode(null);
        t.setMonthlyNth(null);
        t.setMonthlyWeekday(null);
        t.setMonthlyDayOfMonth(null);
        t.setRecurrenceParentId(template.getId());
        t.setRecurrenceInstance(true);
        return t;
    }

    private void addTaskInstanceToSameSections(String templateTaskId, String instanceTaskId) {
        List<Project> projects = projectService.getProjects();
        for (Project p : projects) {
            if (p.getSection() == null) continue;
            boolean changed = false;
            for (Section s : p.getSection()) {
                if (s.getTaskIds() != null && s.getTaskIds().contains(templateTaskId)) {
                    s.getTaskIds().add(instanceTaskId);
                    changed = true;
                }
            }
            if (changed) projectService.save(p);
        }
    }

    private void cloneSubTasksForInstance(Task template, Task instance, Date occStart, Date occEnd) {
        if (template.getSubTaskIds() == null) return;
        List<String> newIds = new ArrayList<>();
        for (String stId : template.getSubTaskIds()) {
            SubTask orig = null;
            try { orig = subTaskRepo.findById(stId).orElse(null); } catch (Exception ignored) {}
            if (orig == null) continue;
            SubTask clone = new SubTask();
            clone.setCreatedId(orig.getCreatedId());
            clone.setProjectIds(orig.getProjectIds());
            clone.setTaskName(orig.getTaskName());
            clone.setCompleted(false);
            clone.setPriority(orig.getPriority());
            clone.setStatus(orig.getStatus());
            clone.setGroup(orig.getGroup());
            clone.setDate(occStart);
            clone.setDescription(orig.getDescription());
            clone.setAssignId(new ArrayList<>(orig.getAssignId()));
            clone.setStartDate(occStart);
            clone.setEndDate(occEnd);
            SubTask saved = subTaskRepo.save(clone);
            newIds.add(saved.getId());
        }
        instance.getSubTaskIds().addAll(newIds);
        save(instance);
    }

    private boolean sameDay(Date a, Date b) {
        Calendar ca = Calendar.getInstance(); ca.setTime(a);
        Calendar cb = Calendar.getInstance(); cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private List<Date> generateWeeklyOccurrences(Date start, Date end, List<Integer> daysOfWeek) {
        List<Date> result = new ArrayList<>();
        if (daysOfWeek == null || daysOfWeek.isEmpty()) return result;
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        // Normalize to start date at 00:00 of that day
        while (!cal.getTime().after(end)) {
            int dow = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0..6
            if (daysOfWeek.contains(dow)) {
                result.add(cal.getTime());
            }
            cal.add(Calendar.DATE, 1);
        }
        return result;
    }

    private List<Date> generateMonthlyNthWeekdayOccurrences(Date start, Date end, Integer nth, Integer weekday) {
        List<Date> result = new ArrayList<>();
        if (nth == null || weekday == null) return result;
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        while (!cal.getTime().after(end)) {
            Date d = nthWeekdayOfMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), nth, weekday);
            if (d != null && !d.before(start) && !d.after(end)) result.add(d);
            cal.add(Calendar.MONTH, 1);
        }
        return result;
    }

    private Date nthWeekdayOfMonth(int year, int monthZeroBased, int nth, int weekdayZeroSun) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, monthZeroBased);
        c.set(Calendar.DAY_OF_MONTH, 1);
        int count = 0;
        while (c.get(Calendar.MONTH) == monthZeroBased) {
            int dow = c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0..6
            if (dow == weekdayZeroSun) {
                count++;
                if (count == nth) return c.getTime();
            }
            c.add(Calendar.DATE, 1);
        }
        return null;
    }

    private List<Date> generateMonthlyDayOccurrences(Date start, Date end, Integer dayOfMonth) {
        List<Date> result = new ArrayList<>();
        if (dayOfMonth == null) return result;
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        while (!cal.getTime().after(end)) {
            Calendar c = (Calendar) cal.clone();
            c.set(Calendar.DAY_OF_MONTH, Math.min(dayOfMonth, c.getActualMaximum(Calendar.DAY_OF_MONTH)));
            Date d = c.getTime();
            if (!d.before(start) && !d.after(end)) result.add(d);
            cal.add(Calendar.MONTH, 1);
        }
        return result;
    }
}
