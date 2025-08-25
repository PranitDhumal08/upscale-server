package com.upscale.upscale.controller;

import com.upscale.upscale.dto.user.*;
import com.upscale.upscale.entity.workspace.Workspace;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.service.*;
import com.upscale.upscale.service.Workspace.WorkspaceService;
import com.upscale.upscale.service.project.EmailService;
import com.upscale.upscale.service.project.GoalService;
import com.upscale.upscale.service.project.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private com.upscale.upscale.service.project.TaskService taskService;

    @PostMapping("/login-user")
    public ResponseEntity<?> loginUser(@RequestBody LoginUser loginUser) {

        try{
            HashMap<String,Object> response = new HashMap<>();

            if(userService.login(loginUser)){
                response.put("status", "success");
                response.put("user", loginUser.getEmail());

                response.put("isNewUser", false);

                String token = tokenService.generateToken(loginUser.getEmail());
                response.put("token", token);

                return new ResponseEntity<>(response, HttpStatus.OK);

            }else if(!userService.checkUserExists(loginUser.getEmail())){
                response.put("status", "error");
                response.put("message", "Email does not exist");
                response.put("isNewUser", true);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("status", "fail");
                response.put("message", "Invalid email or password");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }


        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        }

    }

//    @GetMapping("/check-user/{emailId}")
//    public ResponseEntity<?> checkUserExists(@PathVariable String emailId) {
//        try{
//            HashMap<String, Object> response = new HashMap<>();
//
//            if(userService.checkUserExists(emailId)){
//                response.put("message", "User exists, Otp sent to " + emailId + " successfully. Please check your email for OTP.");
//                response.put("email", emailId);
//                response.put("isNewUser", "false");
//                UserLogin userLogin = new UserLogin();
//                userLogin.setEmailId(emailId);
//                sendOtp(userLogin);
//                return new ResponseEntity<>(response, HttpStatus.OK);
//            }
//            else{
//
//                response.put("message", "User does not exist");
//                response.put("email", emailId);
//                response.put("isNewUser", "true");
//                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//            }
//
//
//        }catch (Exception e){
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody UserLogin user) {

        try {
            String emailId = user.getEmailId();
            if (emailId == null || emailId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email ID is required");
            }

            String otp = String.valueOf(userService.generateOtp());

            User existingUser = userService.getUser(emailId);
            Map<String, String> response = new HashMap<>();

            if (existingUser == null) {
                User newUser = new User();
                newUser.setEmailId(emailId);
                newUser.setOtp(otp);
                newUser.setNewUser(true);
                // Initialize trial system for new users
                newUser.setTrial(14);
                newUser.setActive(true);
                response.put("isNewUser", "true");
                userService.save(newUser);
                log.info("User created: " + emailId + " successfully with 14-day trial "+otp);
            } else {
                existingUser.setOtp(otp);
                existingUser.setNewUser(false);
                userService.save(existingUser);
                response.put("isNewUser", "false");
                log.info("User updated: " + emailId + "suceessfully "+otp);
            }

            //emailService.sendOtpEmail(emailId, otp);


            response.put("message", "OTP sent successfully");
            response.put("email", emailId);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Failed to send OTP: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody UserLoginData userLoginData) {
        try {
            if (userLoginData != null) {
                String emailId = userLoginData.getEmailId();
                String otp = userLoginData.getOtp();

                if (userService.findByEmailIdAndOtp(emailId, otp)) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "OTP verified successfully");
                    response.put("email", emailId);

                    if (userService.isNewUser(emailId)) {
                        response.put("isNewUser", "true");
                    } else {
                        response.put("isNewUser", "false");
                    }

                    // Generate JWT token
                    String token = tokenService.generateToken(emailId);
                    response.put("token", token);

                    return new ResponseEntity<>(response, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(HttpServletRequest request, @RequestBody UserCreate userCreate){
        try {

            //set all data;
            String email = tokenService.getEmailFromToken(request);

            User user = userService.getUserDetails(email, userCreate);

            Workspace workspace = workspaceService.createWorkspace(user.getId());

            user.setWorkspaces(workspace.getId());

            userService.save(user);

            log.info("User Updated: " + email + " successfully");
            HashMap<String, Object> response = new HashMap<>();

            response.put("message", "User created successfully");
            response.put("workspace", workspace.getName());



            response.put("Name", userCreate.getFullName());
            response.put("email", email);
            response.put("token", tokenService.generateToken(email));
            return new ResponseEntity<>(response, HttpStatus.OK);

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(HttpServletRequest request){
        try {

            //set all data;
            String emailId = tokenService.getEmailFromToken(request);

            User user = userService.getUser(emailId);

            if(user != null){

                Map<String, Object> response = new HashMap<>();

                response.put("Email", user.getEmailId());
                response.put("FullName", user.getFullName());
                response.put("Role", user.getRole());
                response.put("Workspaces", user.getWorkspaces());
                response.put("AsanaUsed",user.getAsanaUsed());

                return new ResponseEntity<>(response, HttpStatus.OK);
            }


        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/home")
    public ResponseEntity<?> getHome(HttpServletRequest request){
        try{
            String emailId = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();

            User user = userService.getUser(emailId);
            
            // Update trial status before returning home data
            userService.updateTrialStatus(user);

            response.put("Email", emailId);
            response.put("Time", userService.getDate());
            response.put("FullName", userService.getName(emailId));
            response.put("Role", user.getRole());

            // Add trial information
            response.put("Trial", user.getTrial());
            response.put("Active", user.isActive());

            response.put("Goal", goalService.getMyGoals(emailId));

            // Projects created by the user
            response.put("My Projects", userService.getProjects(emailId));

            // Projects where user is a teammate
            response.put("Teammate Projects", projectService.getProjectsAsTeammate(emailId));

            response.put("Team Mates", userService.getTeamMates(emailId));

            // ✅ NEW: Add weekly and monthly statistics
            response.put("My Week", calculateWeeklyStats(emailId, user.getId()));
            response.put("My Month", calculateMonthlyStats(emailId, user.getId()));

            return new ResponseEntity<>(response, HttpStatus.OK);

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/profile-update")
    public ResponseEntity<?> updateUserProfile(HttpServletRequest request, @RequestBody UserProfileUpdate userProfileUpdate){

        try {
            String emailId = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();

            if(userProfileUpdate != null){

                if(userService.updateUserProfile(emailId, userProfileUpdate)){

                    response.put("message", "Profile updated successfully");
                    response.put("email", emailId);
                    log.info("Profile updated successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("message", "Profile update failed, Not Found");
                    response.put("email", emailId);
                    log.info("Profile update failed, Not Found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }

            }
            else{
                response.put("message", "Profile update failed, Not Found");
                response.put("email", emailId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        }catch (Exception e){
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get-profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request){
        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if(userService.getUserProfileUpdate(emailId) != null){
                UserProfileUpdate userProfileUpdate = userService.getUserProfileUpdate(emailId);
                response.put("Data", userProfileUpdate);
                response.put("email", emailId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/decrease-trial")
    public ResponseEntity<?> decreaseTrial(HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if (userService.decreaseTrialDay(emailId)) {
                User user = userService.getUser(emailId);
                response.put("message", "Trial decreased successfully");
                response.put("trial", user.getTrial());
                response.put("active", user.isActive());
                response.put("status", user.isActive() ? "Active" : "Expired");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "Failed to decrease trial or trial already at 0");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/trial-info")
    public ResponseEntity<?> getTrialInfo(HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = userService.getTrialInfo(emailId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ===================== Forgot Password Flow =====================
    // 1) Initiate: user enters email; if exists, generate OTP and log to terminal (NO JWT here)
    @PostMapping("/forgot/initiate")
    public ResponseEntity<?> forgotInitiate(@RequestBody Map<String, String> payload) {
        try {
            String emailId = payload.get("email");
            if (emailId == null || emailId.trim().isEmpty()) {
                return new ResponseEntity<>("Email is required", HttpStatus.BAD_REQUEST);
            }

            if (!userService.checkUserExists(emailId)) {
                HashMap<String, Object> res = new HashMap<>();
                res.put("message", "User not found");
                res.put("email", emailId);
                return new ResponseEntity<>(res, HttpStatus.NOT_FOUND);
            }

            String otp = String.valueOf(userService.generateOtp());
            boolean saved = userService.setOtpForEmail(emailId, otp);
            if (!saved) {
                return new ResponseEntity<>("Failed to set OTP", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Print OTP to terminal/logs as requested
            log.info("Forgot Password OTP for {}: {}", emailId, otp);

            HashMap<String, Object> res = new HashMap<>();
            res.put("message", "OTP generated. Now call /api/users/forgot/verify-otp with email and otp.");
            res.put("email", emailId);
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 2) Verify OTP: PUBLIC. Accepts email + otp, and returns JWT if correct
    @PostMapping("/forgot/verify-otp")
    public ResponseEntity<?> forgotVerifyOtp(@RequestBody Map<String, String> payload) {
        try {
            String emailId = payload.get("email");
            String otp = payload.get("otp");
            if (emailId == null || emailId.trim().isEmpty()) {
                return new ResponseEntity<>("Email is required", HttpStatus.BAD_REQUEST);
            }
            if (otp == null || otp.trim().isEmpty()) {
                return new ResponseEntity<>("OTP is required", HttpStatus.BAD_REQUEST);
            }

            if (userService.findByEmailIdAndOtp(emailId, otp)) {
                HashMap<String, Object> res = new HashMap<>();
                res.put("message", "OTP verified successfully");
                res.put("email", emailId);
                // Issue JWT to be used for reset-password
                res.put("token", tokenService.generateToken(emailId));
                return new ResponseEntity<>(res, HttpStatus.OK);
            }

            return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 3) Reset Password: requires JWT
    @PostMapping("/forgot/reset-password")
    public ResponseEntity<?> forgotResetPassword(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            if (emailId == null || emailId.isEmpty()) {
                return new ResponseEntity<>("Invalid or missing token", HttpStatus.UNAUTHORIZED);
            }
            String newPassword = payload.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return new ResponseEntity<>("New password is required", HttpStatus.BAD_REQUEST);
            }

            if (newPassword.length() < 6) {
                return new ResponseEntity<>("Password must be at least 6 characters", HttpStatus.BAD_REQUEST);
            }

            boolean ok = userService.resetPassword(emailId, newPassword);
            if (!ok) {
                return new ResponseEntity<>("Failed to reset password", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            HashMap<String, Object> res = new HashMap<>();
            res.put("message", "Password reset successfully");
            res.put("email", emailId);
            // Optionally, issue a fresh token
            res.put("token", tokenService.generateToken(emailId));
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Calculate weekly statistics for tasks completed and collaborators
     * @param emailId User's email ID
     * @param userId User's ID
     * @return Map containing weekly statistics
     */
    private Map<String, Object> calculateWeeklyStats(String emailId, String userId) {
        Map<String, Object> weeklyStats = new HashMap<>();
        
        try {
            // Calculate date range for current week (Monday to Sunday)
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
            java.time.LocalDate endOfWeek = today.with(java.time.DayOfWeek.SUNDAY);
            
            // Convert to Date objects for comparison
            java.util.Date weekStart = java.sql.Date.valueOf(startOfWeek);
            java.util.Date weekEnd = java.sql.Date.valueOf(endOfWeek.plusDays(1)); // Include end day
            
            // Get all tasks assigned to or created by the user
            java.util.List<com.upscale.upscale.entity.project.Task> userTasks = taskService.getTasksByAssignId(userId);
            java.util.List<com.upscale.upscale.entity.project.Task> createdTasks = taskService.getTasksByCreatedId(userId);
            
            // Combine tasks and remove duplicates
            java.util.Set<String> taskIds = new java.util.HashSet<>();
            java.util.List<com.upscale.upscale.entity.project.Task> allTasks = new java.util.ArrayList<>();
            
            for (com.upscale.upscale.entity.project.Task task : userTasks) {
                if (taskIds.add(task.getId())) {
                    allTasks.add(task);
                }
            }
            for (com.upscale.upscale.entity.project.Task task : createdTasks) {
                if (taskIds.add(task.getId())) {
                    allTasks.add(task);
                }
            }
            
            // Count completed tasks in current week
            int weeklyCompletedTasks = 0;
            for (com.upscale.upscale.entity.project.Task task : allTasks) {
                if (task.isCompleted() && isTaskInDateRange(task, weekStart, weekEnd)) {
                    weeklyCompletedTasks++;
                }
            }
            
            // Calculate collaborators (unique users from projects where current user is involved)
            java.util.Set<String> collaborators = getUniqueCollaborators(emailId, userId);
            
            weeklyStats.put("tasksCompleted", weeklyCompletedTasks);
            weeklyStats.put("collaborators", collaborators.size());
            weeklyStats.put("period", "This Week");
            weeklyStats.put("startDate", startOfWeek.toString());
            weeklyStats.put("endDate", endOfWeek.toString());
            
            log.info("Weekly stats for user {}: {} tasks completed, {} collaborators", 
                    emailId, weeklyCompletedTasks, collaborators.size());
                    
        } catch (Exception e) {
            log.error("Error calculating weekly stats for user {}: {}", emailId, e.getMessage());
            weeklyStats.put("tasksCompleted", 0);
            weeklyStats.put("collaborators", 0);
            weeklyStats.put("period", "This Week");
            weeklyStats.put("error", "Unable to calculate weekly statistics");
        }
        
        return weeklyStats;
    }

    /**
     * Calculate monthly statistics for tasks completed and collaborators
     * @param emailId User's email ID
     * @param userId User's ID
     * @return Map containing monthly statistics
     */
    private Map<String, Object> calculateMonthlyStats(String emailId, String userId) {
        Map<String, Object> monthlyStats = new HashMap<>();
        
        try {
            // Calculate date range for current month
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate startOfMonth = today.withDayOfMonth(1);
            java.time.LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            
            // Convert to Date objects for comparison
            java.util.Date monthStart = java.sql.Date.valueOf(startOfMonth);
            java.util.Date monthEnd = java.sql.Date.valueOf(endOfMonth.plusDays(1)); // Include end day
            
            // Get all tasks assigned to or created by the user
            java.util.List<com.upscale.upscale.entity.project.Task> userTasks = taskService.getTasksByAssignId(userId);
            java.util.List<com.upscale.upscale.entity.project.Task> createdTasks = taskService.getTasksByCreatedId(userId);
            
            // Combine tasks and remove duplicates
            java.util.Set<String> taskIds = new java.util.HashSet<>();
            java.util.List<com.upscale.upscale.entity.project.Task> allTasks = new java.util.ArrayList<>();
            
            for (com.upscale.upscale.entity.project.Task task : userTasks) {
                if (taskIds.add(task.getId())) {
                    allTasks.add(task);
                }
            }
            for (com.upscale.upscale.entity.project.Task task : createdTasks) {
                if (taskIds.add(task.getId())) {
                    allTasks.add(task);
                }
            }
            
            // Count completed tasks in current month
            int monthlyCompletedTasks = 0;
            for (com.upscale.upscale.entity.project.Task task : allTasks) {
                if (task.isCompleted() && isTaskInDateRange(task, monthStart, monthEnd)) {
                    monthlyCompletedTasks++;
                }
            }
            
            // Calculate collaborators (unique users from projects where current user is involved)
            java.util.Set<String> collaborators = getUniqueCollaborators(emailId, userId);
            
            monthlyStats.put("tasksCompleted", monthlyCompletedTasks);
            monthlyStats.put("collaborators", collaborators.size());
            monthlyStats.put("period", "This Month");
            monthlyStats.put("startDate", startOfMonth.toString());
            monthlyStats.put("endDate", endOfMonth.toString());
            
            log.info("Monthly stats for user {}: {} tasks completed, {} collaborators", 
                    emailId, monthlyCompletedTasks, collaborators.size());
                    
        } catch (Exception e) {
            log.error("Error calculating monthly stats for user {}: {}", emailId, e.getMessage());
            monthlyStats.put("tasksCompleted", 0);
            monthlyStats.put("collaborators", 0);
            monthlyStats.put("period", "This Month");
            monthlyStats.put("error", "Unable to calculate monthly statistics");
        }
        
        return monthlyStats;
    }

    /**
     * Check if a task falls within the specified date range
     * @param task The task to check
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return true if task is in range, false otherwise
     */
    private boolean isTaskInDateRange(com.upscale.upscale.entity.project.Task task, java.util.Date startDate, java.util.Date endDate) {
        java.util.Date taskDate = task.getDate();
        if (taskDate == null) {
            taskDate = task.getStartDate();
        }
        if (taskDate == null) {
            return false; // No date information available
        }
        
        return taskDate.compareTo(startDate) >= 0 && taskDate.compareTo(endDate) < 0;
    }

    /**
     * Get unique collaborators for a user across all their projects
     * @param emailId User's email ID
     * @param userId User's ID
     * @return Set of unique collaborator user IDs
     */
    private java.util.Set<String> getUniqueCollaborators(String emailId, String userId) {
        java.util.Set<String> collaborators = new java.util.HashSet<>();
        
        try {
            // Get projects created by the user
            java.util.HashMap<String, java.util.List<String>> myProjects = userService.getProjects(emailId);
            
            // Get projects where user is a teammate
            java.util.HashMap<String, String> teammateProjects = projectService.getProjectsAsTeammate(emailId);
            
            // Process projects created by user
            if (myProjects != null) {
                for (String projectId : myProjects.keySet()) {
                    com.upscale.upscale.entity.project.Project project = projectService.getProject(projectId);
                    if (project != null && project.getTeammates() != null) {
                        for (java.util.Map.Entry<String, String[]> entry : project.getTeammates().entrySet()) {
                            String[] teammateInfo = entry.getValue();
                            if (teammateInfo.length > 2) {
                                String teammateEmail = teammateInfo[2]; // Email is at index 2
                                if (!teammateEmail.equals(emailId)) { // Don't count self
                                    User collaborator = userService.getUser(teammateEmail);
                                    if (collaborator != null) {
                                        collaborators.add(collaborator.getId());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Process projects where user is a teammate
            if (teammateProjects != null) {
                for (String projectId : teammateProjects.keySet()) {
                    com.upscale.upscale.entity.project.Project project = projectService.getProject(projectId);
                    if (project != null) {
                        // Add project owner as collaborator
                        if (!project.getUserEmailid().equals(emailId)) {
                            User owner = userService.getUser(project.getUserEmailid());
                            if (owner != null) {
                                collaborators.add(owner.getId());
                            }
                        }
                        
                        // Add other teammates as collaborators
                        if (project.getTeammates() != null) {
                            for (java.util.Map.Entry<String, String[]> entry : project.getTeammates().entrySet()) {
                                String[] teammateInfo = entry.getValue();
                                if (teammateInfo.length > 2) {
                                    String teammateEmail = teammateInfo[2]; // Email is at index 2
                                    if (!teammateEmail.equals(emailId)) { // Don't count self
                                        User collaborator = userService.getUser(teammateEmail);
                                        if (collaborator != null) {
                                            collaborators.add(collaborator.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error calculating collaborators for user {}: {}", emailId, e.getMessage());
        }
        
        return collaborators;
    }

    /**
     * Delete user account and all associated data
     * This endpoint handles complete user deletion including cascading deletes
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteUserAccount(HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if (userService.deleteUser(emailId)) {
                response.put("message", "User account and all associated data deleted successfully");
                response.put("email", emailId);
                response.put("status", "success");
                log.info("User account deleted successfully: {}", emailId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "Failed to delete user account");
                response.put("email", emailId);
                response.put("status", "error");
                log.error("Failed to delete user account: {}", emailId);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Error occurred while deleting user account: " + e.getMessage());
            response.put("status", "error");
            log.error("Error deleting user account: {}", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete user by email ID (Admin endpoint)
     * This endpoint allows deletion of any user by email ID
     */
    @DeleteMapping("/delete-user/{emailId}")
    public ResponseEntity<?> deleteUserByEmail(@PathVariable String emailId) {
        try {
            HashMap<String, Object> response = new HashMap<>();

            if (!userService.checkUserExists(emailId)) {
                response.put("message", "User not found");
                response.put("email", emailId);
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (userService.deleteUser(emailId)) {
                response.put("message", "User deleted successfully");
                response.put("email", emailId);
                response.put("status", "success");
                log.info("User deleted successfully by admin: {}", emailId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "Failed to delete user");
                response.put("email", emailId);
                response.put("status", "error");
                log.error("Failed to delete user by admin: {}", emailId);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Error occurred while deleting user: " + e.getMessage());
            response.put("status", "error");
            log.error("Error deleting user by admin: {}", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete user by user ID (Admin endpoint)
     * This endpoint allows deletion of any user by user ID
     */
    @DeleteMapping("/delete-user-by-id/{userId}")
    public ResponseEntity<?> deleteUserById(@PathVariable String userId) {
        try {
            HashMap<String, Object> response = new HashMap<>();

            User user = userService.getUserById(userId);
            if (user == null) {
                response.put("message", "User not found");
                response.put("userId", userId);
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (userService.deleteUserById(userId)) {
                response.put("message", "User deleted successfully");
                response.put("userId", userId);
                response.put("email", user.getEmailId());
                response.put("status", "success");
                log.info("User deleted successfully by admin (ID: {}): {}", userId, user.getEmailId());
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "Failed to delete user");
                response.put("userId", userId);
                response.put("status", "error");
                log.error("Failed to delete user by admin (ID: {})", userId);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            HashMap<String, Object> response = new HashMap<>();
            response.put("message", "Error occurred while deleting user: " + e.getMessage());
            response.put("status", "error");
            log.error("Error deleting user by admin (ID: {}): {}", userId, e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
