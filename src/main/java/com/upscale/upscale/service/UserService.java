package com.upscale.upscale.service;

import com.upscale.upscale.dto.user.LoginUser;
import com.upscale.upscale.dto.user.UserCreate;
import com.upscale.upscale.dto.user.UserProfileUpdate;
import com.upscale.upscale.entity.workspace.Workspace;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.UserRepo;
import com.upscale.upscale.service.Workspace.WorkspaceService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    @Lazy
    private PortfolioService portfolioService;

    @Autowired
    @Lazy
    private WorkspaceService workspaceService;

    @Autowired
    @Lazy
    private com.upscale.upscale.service.project.ProjectService projectService;

    private PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public UserService() {
        // Set default password encoder to bcrypt
        System.setProperty("spring.security.password.encoder", "bcrypt");
    }

    public boolean checkUserExists(String emailId) {
        return userRepo.findByEmailId(emailId) != null;
    }

    public int generateOtp() {
        Random random = new Random();
        return 100000 + random.nextInt(900000); // Generates a 6-digit OTP
    }

    public boolean login(LoginUser loginUser) {
        User user = userRepo.findByEmailId(loginUser.getEmail());
        if(user != null) {
            if(passwordEncoder.matches(loginUser.getPassword(),user.getPassword())) return true;
        }
        return false;
    }

    public void save(User user) {
        userRepo.save(user);
    }

    public boolean findByEmailIdAndOtp(String emailId,String otp) {
        User user =  userRepo.findByEmailIdAndOtp( emailId, otp);
        if(user != null){
            if(user.getEmailId().equals(emailId) && user.getOtp().equals(otp)){
                return true;
            }
        }
        return false;
    }
    public boolean isNewUser(String emailId) {
        return userRepo.findByEmailId(emailId).isNewUser();
    }

    public User getUser(String emailId){
        return userRepo.findByEmailId(emailId);
    }



    public User getUserDetails(String emailId, UserCreate userCreate){

        User user = getUser(emailId);

        user.setFullName(userCreate.getFullName());
        user.setAsanaUsed(userCreate.getAsanaUsed());
        user.setRole(userCreate.getRole());
        user.setNewUser(false);
        user.setPassword(passwordEncoder.encode(userCreate.getPassword()));

        user.setOtp("");

        save(user);

        User user1 = userRepo.findByEmailId(emailId);

        return user1;

    }

    public String getName(String emailId){
        User user = getUser(emailId);
        if(user != null) return user.getFullName();

        return "";
    }
    public String getDate(){
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d");
        return today.format(formatter);
    }
    public boolean setProject(Project project,String emailId){
        User user = getUser(emailId);
        if(user != null){
            user.getProjects().add(project.getId()); // Add project ID, not project object
            save(user);
            return true;
        }
        return false;
    }

    public List<String> getTeamMates(String emailId) {
        User user = getUser(emailId);;
        if(user != null){
            return user.getTeammates();
        }
        return new ArrayList<>();
    }

    public HashMap<String,List<String>> getProjects(String emailId) {

        HashMap<String,List<String>> map = new HashMap<>();

        User user = getUser(emailId);
        if(user != null){
            List<String> projectIds = user.getProjects(); // Get project IDs, not project objects
            if(projectIds != null){
                for(String projectId : projectIds){
                    Project project = projectService.getProject(projectId);
                    if(project != null) {
                        List<String> values = new ArrayList<>();
                        values.add(project.getProjectName()); // Get actual project name
                        values.add("project");
                        map.put(projectId, values);
                    }
                }
            }

            List<Portfolio> portfolio = portfolioService.getPortFolio(emailId);

            if(portfolio != null){
                for(Portfolio p : portfolio){
                    List<String> values = new ArrayList<>();
                    values.add(p.getPortfolioName());
                    values.add("portfolio");

                    map.put(p.getId(),values);
                }
            }

            Workspace workspace = workspaceService.getWorkspace(user.getId());

            if(workspace != null){
                List<String> values = new ArrayList<>();
                values.add(workspace.getName());
                values.add("workspace");
                map.put(workspace.getId(),values);
            }

            return map;
        }

        return map;
    }

    public User getUserById(String id) {
        return userRepo.findById(id).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public boolean updateUserProfile(String emailId, UserProfileUpdate userProfileUpdate){

        User user = getUser(emailId);

        if(user != null){

            if(!userProfileUpdate.getFullName().isEmpty()) user.setFullName(userProfileUpdate.getFullName());
            if(!userProfileUpdate.getPronouns().isEmpty()) user.setPronouns(userProfileUpdate.getPronouns());
            if(!userProfileUpdate.getJobTitle().isEmpty()) user.setJobTitle(userProfileUpdate.getJobTitle());
            if(!userProfileUpdate.getDepartmentOrTeam().isEmpty()) user.setDepartmentOrTeam(userProfileUpdate.getDepartmentOrTeam());
            if(!userProfileUpdate.getRole().isEmpty()) user.setRole(userProfileUpdate.getRole());
            if(!userProfileUpdate.getAboutMe().isEmpty()) user.setAboutMe(userProfileUpdate.getAboutMe());

            log.info("Updating user profile"+user);
            userRepo.save(user);
            return true;

        }

        return false;
    }

    public UserProfileUpdate getUserProfileUpdate(String emailId) {
        User user = getUser(emailId);
        if(user != null){

            UserProfileUpdate userProfileUpdate = new UserProfileUpdate();
            userProfileUpdate.setFullName(user.getFullName());
            userProfileUpdate.setPronouns(user.getPronouns());
            userProfileUpdate.setJobTitle(user.getJobTitle());
            userProfileUpdate.setDepartmentOrTeam(user.getDepartmentOrTeam());
            userProfileUpdate.setRole(user.getRole());
            userProfileUpdate.setAboutMe(user.getAboutMe());
            return userProfileUpdate;

        }
        return null;
    }

    /**
     * Updates the trial status for a user based on their creation date
     * This method is called when user accesses home API
     */
    public void updateTrialStatus(User user) {
        if (user == null) return;
        
        // Only update if user is currently active
        if (user.isActive() && user.getTrial() > 0) {
            // For now, we'll manually decrease trial when this method is called
            // In a real implementation, you'd track the last update date
            log.info("Current trial status for user {}: {} days remaining, active: {}", 
                    user.getEmailId(), user.getTrial(), user.isActive());
        }
        
        // If trial has reached 0, deactivate the user
        if (user.getTrial() <= 0 && user.isActive()) {
            user.setActive(false);
            save(user);
            log.info("User {} trial expired. Account deactivated.", user.getEmailId());
        }
    }

    /**
     * Manually decrease trial for a user (for testing purposes)
     * In production, this would be handled by a scheduled task
     */
    public boolean decreaseTrialDay(String emailId) {
        User user = getUser(emailId);
        if (user != null && user.getTrial() > 0) {
            user.setTrial(user.getTrial() - 1);
            
            // If trial reaches 0, deactivate user
            if (user.getTrial() <= 0) {
                user.setActive(false);
                log.info("User {} trial expired after manual decrease. Account deactivated.", emailId);
            }
            
            save(user);
            log.info("Trial decreased for user {}: {} days remaining", emailId, user.getTrial());
            return true;
        }
        return false;
    }

    /**
     * Scheduled task to decrease trial for all active users daily
     * Runs every day at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?") // Runs at 00:00:00 every day
    public void dailyTrialUpdate() {
        log.info("Starting daily trial update for all users...");
        
        List<User> allUsers = getAllUsers();
        int updatedUsers = 0;
        int deactivatedUsers = 0;
        
        for (User user : allUsers) {
            if (user.isActive() && user.getTrial() > 0) {
                user.setTrial(user.getTrial() - 1);
                
                if (user.getTrial() <= 0) {
                    user.setActive(false);
                    deactivatedUsers++;
                    log.info("User {} trial expired. Account deactivated.", user.getEmailId());
                } else {
                    log.debug("Trial decreased for user {}: {} days remaining", user.getEmailId(), user.getTrial());
                }
                
                save(user);
                updatedUsers++;
            }
        }
        
        log.info("Daily trial update completed. Updated {} users, deactivated {} users", updatedUsers, deactivatedUsers);
    }

    /**
     * Get trial information for a user
     */
    public HashMap<String, Object> getTrialInfo(String emailId) {
        User user = getUser(emailId);
        HashMap<String, Object> trialInfo = new HashMap<>();
        
        if (user != null) {
            trialInfo.put("trial", user.getTrial());
            trialInfo.put("active", user.isActive());
            trialInfo.put("status", user.isActive() ? "Active" : "Expired");
        } else {
            trialInfo.put("error", "User not found");
        }
        
        return trialInfo;
    }

    /**
     * Delete a user and all related data
     * This method handles cascading deletes for projects, portfolios, and workspaces
     */
    public boolean deleteUser(String emailId) {
        try {
            User user = getUser(emailId);
            if (user == null) {
                log.error("User not found for deletion: {}", emailId);
                return false;
            }

            String userId = user.getId();
            log.info("Starting deletion process for user: {} (ID: {})", emailId, userId);

            // 1. Delete all projects created by the user
            List<String> projectIds = user.getProjects();
            if (projectIds != null && !projectIds.isEmpty()) {
                for (String projectId : projectIds) {
                    try {
                        projectService.deleteProject(projectId);
                        log.info("Deleted project: {}", projectId);
                    } catch (Exception e) {
                        log.error("Error deleting project {}: {}", projectId, e.getMessage());
                    }
                }
            }

            // 2. Remove user from projects where they are teammates
            try {
                projectService.removeUserFromAllProjects(emailId);
                log.info("Removed user from all teammate projects");
            } catch (Exception e) {
                log.error("Error removing user from teammate projects: {}", e.getMessage());
            }

            // 3. Delete all portfolios owned by the user
            try {
                List<Portfolio> userPortfolios = portfolioService.getPortFolio(emailId);
                if (userPortfolios != null && !userPortfolios.isEmpty()) {
                    for (Portfolio portfolio : userPortfolios) {
                        portfolioService.deletePortfolio(portfolio.getId());
                        log.info("Deleted portfolio: {}", portfolio.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Error deleting user portfolios: {}", e.getMessage());
            }

            // 4. Delete workspace owned by the user
            try {
                Workspace workspace = workspaceService.getWorkspace(userId);
                if (workspace != null) {
                    workspaceService.deleteWorkspace(workspace.getId());
                    log.info("Deleted workspace: {}", workspace.getId());
                }
            } catch (Exception e) {
                log.error("Error deleting user workspace: {}", e.getMessage());
            }

            // 5. Remove user from other users' teammates lists
            try {
                removeUserFromTeammatesLists(emailId);
                log.info("Removed user from all teammates lists");
            } catch (Exception e) {
                log.error("Error removing user from teammates lists: {}", e.getMessage());
            }

            // 6. Finally, delete the user
            userRepo.delete(user);
            log.info("Successfully deleted user: {}", emailId);
            
            return true;

        } catch (Exception e) {
            log.error("Error deleting user {}: {}", emailId, e.getMessage());
            return false;
        }
    }

    /**
     * Remove user from all other users' teammates lists
     */
    private void removeUserFromTeammatesLists(String emailId) {
        List<User> allUsers = getAllUsers();
        for (User user : allUsers) {
            if (user.getTeammates() != null && user.getTeammates().contains(emailId)) {
                user.getTeammates().remove(emailId);
                save(user);
                log.debug("Removed {} from {}'s teammates list", emailId, user.getEmailId());
            }
        }
    }

    /**
     * Delete user by ID
     */
    public boolean deleteUserById(String userId) {
        try {
            User user = getUserById(userId);
            if (user != null) {
                return deleteUser(user.getEmailId());
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting user by ID {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
