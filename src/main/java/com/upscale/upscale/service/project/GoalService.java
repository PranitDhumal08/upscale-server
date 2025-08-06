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
                goalData.setUserId(currentUserId);
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

}
