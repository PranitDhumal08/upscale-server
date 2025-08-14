package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.GoalData;
import com.upscale.upscale.entity.Goal;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.GoalRepo;
import com.upscale.upscale.repository.UserRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
@Slf4j
public class GoalService {

    @Autowired
    private GoalRepo goalRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private UserService userService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    public void save(Goal goal) {
        goalRepo.save(goal);
    }

    public Goal saveGoal(Goal goal){
        return goalRepo.save(goal);
    }
    public String userId(String emailId){
        User user = userRepo.findByEmailId(emailId);
        return user.getId();
    }

    public boolean setGoal(String emailId, GoalData goalData){
        if(goalData != null){

            Goal goal = new Goal();

            goal.setUserId(userId(emailId));
            goal.setGoalTitle(goalData.getGoalTitle());
            goal.setGoalOwner(userId(emailId));
            goal.setTimePeriod(goalData.getTimePeriod());
            goal.setPrivacy(goalData.getPrivacy());

//            List<String>  = new ArrayList<>();
//            for(int i=0;i<goalData.getChildren().size();i++){
//
//            }

            List<String> members = new ArrayList<>();
            
            // Always add the goal owner first
            members.add(userId(emailId));

            // If members are provided in the request, add them (making it a team goal)
            if(goalData.getMembers() != null && !goalData.getMembers().isEmpty()){
                for(String memberEmail : goalData.getMembers()){
                    User user = userService.getUser(memberEmail);

                    if(user != null && !user.getId().equals(userId(emailId))){ // Avoid duplicate owner
                        members.add(user.getId());
                        inboxService.sendGoalMessage(goal, emailId, memberEmail);
                        log.info("Send goal message to user {}", user.getFullName());
                    }
                }
                log.info("Created team goal with {} members", members.size());
            } else {
                log.info("Created personal goal for user {}", emailId);
            }

            goal.setMembers(members);
            goal.setProjectIds(goalData.getProjectIds() != null ? goalData.getProjectIds() : new ArrayList<>());

            save(goal);
            return true;
        }
        return false;
    }

    public List<GoalData> getTeamGoal(String emailId){
        String currentUserId = userId(emailId);
        
        // Get all goals where the user is a member
        List<Goal> allGoals = goalRepo.findAll();
        List<GoalData> goalDataList = new ArrayList<>();

        for(Goal dbGoal : allGoals){
            if(dbGoal != null && dbGoal.getMembers().contains(currentUserId)){
                
                // Only include team goals (goals with more than 1 member)
                if(dbGoal.getMembers().size() > 1){

                    GoalData goalData = new GoalData();


                    goalData.setGoalId(dbGoal.getId());
                    goalData.setUserId(currentUserId);
                    goalData.setGoalTitle(dbGoal.getGoalTitle());
                    goalData.setTimePeriod(dbGoal.getTimePeriod());
                    goalData.setPrivacy(dbGoal.getPrivacy());
                    goalData.setProjectIds(dbGoal.getProjectIds());
                    
                    // Convert member IDs to email addresses
                    List<String> memberEmails = new ArrayList<>();
                    for(String memberId : dbGoal.getMembers()){
                        User memberUser = userService.getUserById(memberId);
                        if(memberUser != null){
                            memberEmails.add(memberUser.getEmailId());
                        }
                    }
                    goalData.setMembers(memberEmails);
                    goalData.setGoalOwner(dbGoal.getGoalOwner());

                    // Calculate goal completion percentage based on associated project tasks
                    calculateGoalCompletion(goalData, dbGoal.getProjectIds());

                    goalDataList.add(goalData);
                    log.info("Found team goal: {} with {} members and {}% completion", 
                            dbGoal.getGoalTitle(), dbGoal.getMembers().size(), goalData.getCompletionPercentage());
                }
            }
        }
        
        log.info("Retrieved {} team goals for user {}", goalDataList.size(), emailId);
        return goalDataList;
    }

    public List<GoalData> getMyGoals(String emailId){
        String currentUserId = userId(emailId);
        
        // Get goals created by this user (where they are the owner)
        List<Goal> goals = goalRepo.findByUserId(currentUserId);
        List<GoalData> goalDataList = new ArrayList<>();
        
        for(Goal goal : goals) {
            // Only include personal goals (goals with exactly 1 member - just the owner)
            if(goal.getMembers().contains(currentUserId) && goal.getMembers().size() == 1){
                GoalData goalData = new GoalData();
                goalData.setGoalId(goal.getId());
                goalData.setUserId(goal.getUserId());
                goalData.setGoalTitle(goal.getGoalTitle());
                goalData.setTimePeriod(goal.getTimePeriod());
                goalData.setPrivacy(goal.getPrivacy());
                goalData.setProjectIds(goal.getProjectIds());
                
                // Convert member IDs to email addresses (though it should only be the owner)
                List<String> memberEmails = new ArrayList<>();
                for(String memberId : goal.getMembers()){
                    User memberUser = userService.getUserById(memberId);
                    if(memberUser != null){
                        memberEmails.add(memberUser.getEmailId());
                    }
                }
                goalData.setMembers(memberEmails);
                goalData.setGoalOwner(goal.getGoalOwner());
                
                // Calculate goal completion percentage based on associated project tasks
                calculateGoalCompletion(goalData, goal.getProjectIds());
                
                goalDataList.add(goalData);
                log.info("Found personal goal: {} for user {} with {}% completion", 
                        goal.getGoalTitle(), emailId, goalData.getCompletionPercentage());
            }
        }
        
        log.info("Retrieved {} personal goals for user {}", goalDataList.size(), emailId);
        return goalDataList;
    }

    /**
     * Calculate goal completion percentage based on associated project tasks
     * @param goalData The goal data object to update with completion info
     * @param projectIds List of project IDs associated with the goal
     */
    private void calculateGoalCompletion(GoalData goalData, List<String> projectIds) {
        int totalTasks = 0;
        int completedTasks = 0;
        
        try {
            // If no projects are associated with the goal, set completion to 0%
            if (projectIds == null || projectIds.isEmpty()) {
                goalData.setCompletionPercentage(0.0);
                goalData.setTotalTasks(0);
                goalData.setCompletedTasks(0);
                log.info("Goal '{}' has no associated projects, completion: 0%", goalData.getGoalTitle());
                return;
            }
            
            // Iterate through all associated projects
            for (String projectId : projectIds) {
                try {
                    // Get all tasks for this project
                    List<com.upscale.upscale.entity.project.Task> projectTasks = taskService.getTasksByProjectId(projectId);
                    
                    if (projectTasks != null) {
                        totalTasks += projectTasks.size();
                        
                        // Count completed tasks
                        for (com.upscale.upscale.entity.project.Task task : projectTasks) {
                            if (task.isCompleted()) {
                                completedTasks++;
                            }
                        }
                        
                        log.debug("Project {}: {} total tasks, {} completed", 
                                projectId, projectTasks.size(), 
                                projectTasks.stream().mapToInt(t -> t.isCompleted() ? 1 : 0).sum());
                    }
                } catch (Exception e) {
                    log.warn("Error processing project {} for goal completion: {}", projectId, e.getMessage());
                }
            }
            
            // Calculate completion percentage
            double completionPercentage = 0.0;
            if (totalTasks > 0) {
                completionPercentage = Math.round((double) completedTasks / totalTasks * 100.0 * 100.0) / 100.0; // Round to 2 decimal places
            }
            
            // Set the calculated values
            goalData.setTotalTasks(totalTasks);
            goalData.setCompletedTasks(completedTasks);
            goalData.setCompletionPercentage(completionPercentage);
            
            log.info("Goal '{}' completion calculated: {}/{} tasks completed ({}%)", 
                    goalData.getGoalTitle(), completedTasks, totalTasks, completionPercentage);
                    
        } catch (Exception e) {
            log.error("Error calculating goal completion for goal '{}': {}", goalData.getGoalTitle(), e.getMessage());
            // Set default values in case of error
            goalData.setCompletionPercentage(0.0);
            goalData.setTotalTasks(0);
            goalData.setCompletedTasks(0);
        }
    }



    public boolean setSubGoal(String parentGoalId, GoalData goalData){

        Goal parentGoal = goalRepo.findById(parentGoalId).orElse(null);
        if(parentGoal != null && goalData != null){

            List<String> childGoalIds = parentGoal.getSubGoalIds();

            Goal newGoal = new Goal();
            
            // Set basic goal properties
            newGoal.setGoalTitle(goalData.getGoalTitle());
            newGoal.setTimePeriod(goalData.getTimePeriod());
            newGoal.setPrivacy(goalData.getPrivacy());
            newGoal.setProjectIds(goalData.getProjectIds() != null ? goalData.getProjectIds() : new ArrayList<>());
            
            // Set owner information
            String ownerUserId = userId(goalData.getGoalOwner());
            newGoal.setUserId(ownerUserId);
            newGoal.setGoalOwner(ownerUserId);
            
            // Set members (similar to main goal logic)
            List<String> members = new ArrayList<>();
            members.add(ownerUserId); // Always add owner first
            
            // If members are provided in the request, add them (making it a team sub-goal)
            if(goalData.getMembers() != null && !goalData.getMembers().isEmpty()){
                for(String memberEmail : goalData.getMembers()){
                    User user = userService.getUser(memberEmail);
                    if(user != null && !user.getId().equals(ownerUserId)){ // Avoid duplicate owner
                        members.add(user.getId());
                        // Send notification about sub-goal assignment
                        inboxService.sendGoalMessage(newGoal, goalData.getGoalOwner(), memberEmail);
                        log.info("Send sub-goal message to user {}", user.getFullName());
                    }
                }
                log.info("Created team sub-goal with {} members", members.size());
            } else {
                log.info("Created personal sub-goal for user {}", goalData.getGoalOwner());
            }
            
            newGoal.setMembers(members);

            // Save the new sub-goal
            Goal savedSubGoal = saveGoal(newGoal);

            // Add sub-goal ID to parent goal
            childGoalIds.add(savedSubGoal.getId());
            parentGoal.setSubGoalIds(childGoalIds);
            goalRepo.save(parentGoal);
            
            log.info("Sub-goal '{}' created successfully with ID: {} for parent goal: {}", 
                    goalData.getGoalTitle(), savedSubGoal.getId(), parentGoalId);
            return true;
        }

        log.error("Failed to create sub-goal: Parent goal not found or invalid data");
        return false;
    }

    /**
     * Get all goals with their children (sub-goals) in a hierarchical structure
     * @param emailId The email ID of the user
     * @return List of parent goals with their children populated
     */
    public List<GoalData> getAllGoalsWithChildren(String emailId) {
        String currentUserId = userId(emailId);
        
        try {
            // Get all goals where the user is either owner or member
            List<Goal> allUserGoals = new ArrayList<>();
            
            // Get goals created by this user
            List<Goal> ownedGoals = goalRepo.findByUserId(currentUserId);
            allUserGoals.addAll(ownedGoals);
            
            // Get goals where user is a member (but not owner)
            List<Goal> allGoals = goalRepo.findAll();
            for (Goal goal : allGoals) {
                if (goal.getMembers().contains(currentUserId) && !goal.getUserId().equals(currentUserId)) {
                    allUserGoals.add(goal);
                }
            }
            
            // Separate parent goals and sub-goals
            List<Goal> parentGoals = new ArrayList<>();
            List<Goal> subGoals = new ArrayList<>();
            
            for (Goal goal : allUserGoals) {
                if (isParentGoal(goal, allUserGoals)) {
                    parentGoals.add(goal);
                } else if (isSubGoal(goal, allUserGoals)) {
                    subGoals.add(goal);
                }
            }
            
            // Convert parent goals to GoalData and populate their children
            List<GoalData> result = new ArrayList<>();
            
            for (Goal parentGoal : parentGoals) {
                GoalData parentGoalData = convertGoalToGoalData(parentGoal, currentUserId);
                
                // Find and add children for this parent goal
                if (parentGoal.getSubGoalIds() != null && !parentGoal.getSubGoalIds().isEmpty()) {
                    List<GoalData> children = new ArrayList<>();
                    
                    for (String subGoalId : parentGoal.getSubGoalIds()) {
                        Goal subGoal = goalRepo.findById(subGoalId).orElse(null);
                        if (subGoal != null) {
                            GoalData childGoalData = convertGoalToGoalData(subGoal, currentUserId);
                            childGoalData.setParentGoalId(parentGoal.getId());
                            children.add(childGoalData);
                        }
                    }
                    
                    parentGoalData.setChildren(children);
                    log.info("Parent goal '{}' has {} sub-goals", parentGoal.getGoalTitle(), children.size());
                }
                
                result.add(parentGoalData);
            }
            
            log.info("Retrieved {} parent goals with hierarchical structure for user {}", result.size(), emailId);
            return result;
            
        } catch (Exception e) {
            log.error("Error retrieving goals with children for user {}: {}", emailId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if a goal is a parent goal (has sub-goals or is not referenced as a sub-goal)
     */
    private boolean isParentGoal(Goal goal, List<Goal> allGoals) {
        // A goal is a parent if it has sub-goals OR if it's not referenced as a sub-goal by any other goal
        if (goal.getSubGoalIds() != null && !goal.getSubGoalIds().isEmpty()) {
            return true;
        }
        
        // Check if this goal is referenced as a sub-goal by any other goal
        for (Goal otherGoal : allGoals) {
            if (otherGoal.getSubGoalIds() != null && otherGoal.getSubGoalIds().contains(goal.getId())) {
                return false; // This goal is a sub-goal of another goal
            }
        }
        
        return true; // This goal is not referenced as a sub-goal, so it's a parent goal
    }
    
    /**
     * Check if a goal is a sub-goal (referenced by another goal's subGoalIds)
     */
    private boolean isSubGoal(Goal goal, List<Goal> allGoals) {
        for (Goal otherGoal : allGoals) {
            if (otherGoal.getSubGoalIds() != null && otherGoal.getSubGoalIds().contains(goal.getId())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Convert Goal entity to GoalData DTO with completion calculation
     */
    private GoalData convertGoalToGoalData(Goal goal, String currentUserId) {
        GoalData goalData = new GoalData();
        
        goalData.setGoalId(goal.getId());
        goalData.setUserId(goal.getUserId());
        goalData.setGoalTitle(goal.getGoalTitle());
        goalData.setTimePeriod(goal.getTimePeriod());
        goalData.setPrivacy(goal.getPrivacy());
        goalData.setProjectIds(goal.getProjectIds());
        goalData.setGoalOwner(goal.getGoalOwner());
        
        // Convert member IDs to email addresses
        List<String> memberEmails = new ArrayList<>();
        if (goal.getMembers() != null) {
            for (String memberId : goal.getMembers()) {
                User memberUser = userService.getUserById(memberId);
                if (memberUser != null) {
                    memberEmails.add(memberUser.getEmailId());
                }
            }
        }
        goalData.setMembers(memberEmails);
        
        // Calculate completion percentage
        calculateGoalCompletion(goalData, goal.getProjectIds());
        
        return goalData;
    }

    /**
     * Get all goals that are connected to a specific project
     * @param projectId The project ID to search for
     * @return List of goals that contain this project ID
     */
    public List<GoalData> getGoalsByProjectId(String projectId) {
        List<GoalData> connectedGoals = new ArrayList<>();
        
        try {
            // Get all goals from the database
            List<Goal> allGoals = goalRepo.findAll();
            
            for (Goal goal : allGoals) {
                // Check if this goal contains the specified project ID
                if (goal.getProjectIds() != null && goal.getProjectIds().contains(projectId)) {
                    GoalData goalData = new GoalData();
                    
                    goalData.setGoalId(goal.getId());
                    goalData.setUserId(goal.getUserId());
                    goalData.setGoalTitle(goal.getGoalTitle());
                    goalData.setTimePeriod(goal.getTimePeriod());
                    goalData.setPrivacy(goal.getPrivacy());
                    goalData.setProjectIds(goal.getProjectIds());
                    goalData.setGoalOwner(goal.getGoalOwner());
                    
                    // Convert member IDs to email addresses
                    List<String> memberEmails = new ArrayList<>();
                    if (goal.getMembers() != null) {
                        for (String memberId : goal.getMembers()) {
                            User memberUser = userService.getUserById(memberId);
                            if (memberUser != null) {
                                memberEmails.add(memberUser.getEmailId());
                            }
                        }
                    }
                    goalData.setMembers(memberEmails);
                    
                    // Calculate completion percentage for this goal
                    calculateGoalCompletion(goalData, goal.getProjectIds());
                    
                    connectedGoals.add(goalData);
                    
                    log.info("Found goal '{}' connected to project {} with {}% completion", 
                            goal.getGoalTitle(), projectId, goalData.getCompletionPercentage());
                }
            }
            
            log.info("Found {} goals connected to project {}", connectedGoals.size(), projectId);
            
        } catch (Exception e) {
            log.error("Error retrieving goals for project {}: {}", projectId, e.getMessage());
        }
        
        return connectedGoals;
    }

}
