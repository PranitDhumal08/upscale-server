package com.upscale.upscale.service.portfolio;

import com.upscale.upscale.dto.portfolio.CreatePortFolio;
import com.upscale.upscale.dto.portfolio.FieldAttribute;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.PortfolioRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.project.ProjectService;
import com.upscale.upscale.service.project.TaskService;
import com.upscale.upscale.service.portfolio.FieldService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
@Lazy
public class PortfolioService {

    @Autowired
    private PortfolioRepo portfolioRepo;

    @Autowired
    @Lazy
    private UserService userService;
    @Autowired
    @Lazy
    private ProjectService projectService;
    @Autowired
    @Lazy
    private TaskService taskService;
    @Autowired
    @Lazy
    private FieldService fieldService;

    public void save(Portfolio portfolio) {
        portfolioRepo.save(portfolio);
    }

    public boolean createPortfolio(String emailId, CreatePortFolio createPortFolio) {
        Portfolio portfolio = new Portfolio();

        if(createPortFolio != null) {

            User user = userService.getUser(emailId);

            portfolio.setOwnerId(user.getId());
            portfolio.setPortfolioName(createPortFolio.getPortfolioName());
            portfolio.setPrivacy(createPortFolio.getPrivacy());
            portfolio.setDefaultView(createPortFolio.getDefaultView());
            //portfolio.setTeammates(createPortFolio.getTeammates());

            HashMap<String,String> fields = new HashMap<>();
            fields.put("1","name");
            fields.put("2","status");
            fields.put("3","task progress");
            fields.put("4","due date");
            fields.put("5","priority");
            fields.put("6","owner");

            portfolio.setFields(fields);


            portfolioRepo.save(portfolio);
            log.info("Portfolio created successfully {}", createPortFolio.getPortfolioName());
            return true;
        }
        return false;
    }

    public List<Portfolio> getPortFolio(String emailId) {

        String userId = userService.getUser(emailId).getId();

        return portfolioRepo.findByOwnerId(userId);
    }

    public Optional<Portfolio> getPortfolio(String id) {
        return portfolioRepo.findById(id);
    }

    public String addProjectToPortfolio(String projectId, String portfolioId) {

        Project project = projectService.getProject(projectId);

        if(project != null) {
            Portfolio portfolio = portfolioRepo.findById(portfolioId).get();

            List<String> projectIds = portfolio.getProjectsIds();
            projectIds.add(projectId);
            portfolio.setProjectsIds(projectIds);
            portfolio.getAttributes().put(projectId, null);

            save(portfolio);
            return portfolio.getPortfolioName();
        }
        return null;
    }

    public String addPortfolioToPortfolio(String portfolioId, String reqPortfolioId) {

        Portfolio portfolio = portfolioRepo.findById(reqPortfolioId).get();

        if(portfolio != null) {

            Portfolio mainPortfolio = portfolioRepo.findById(portfolioId).get();
            List<String> portfolioIds = mainPortfolio.getProjectsIds();
            portfolioIds.add(reqPortfolioId);
            mainPortfolio.setProjectsIds(portfolioIds);
            save(mainPortfolio);

            return mainPortfolio.getPortfolioName();
        }
        return null;
    }

    public HashMap<String, Object> getBasicInfo(String portfolioId) {
        HashMap<String, Object> basicInfo = new HashMap<>();
        Portfolio portfolio = portfolioRepo.findById(portfolioId).get();
        basicInfo.put("id", portfolio.getId());
        basicInfo.put("name", portfolio.getPortfolioName());
        basicInfo.put("fields", portfolio.getFields());

        return basicInfo;

    }

    public Map<String, Object> getPortfolioTaskProgress(String portfolioId) {
        Map<String, Object> progressSummary = new HashMap<>();
        Optional<Portfolio> portfolioOpt = getPortfolio(portfolioId);

        if (portfolioOpt.isEmpty()) {
            progressSummary.put("error", "Portfolio not found");
            return progressSummary;
        }

        Portfolio portfolio = portfolioOpt.get();
        List<String> projectIds = portfolio.getProjectsIds();
        Set<String> seenProjectIds = new HashSet<>();
        List<Map<String, Object>> projectProgressList = new ArrayList<>();

        // Get all fieldWiseData for all projects (field name → value)
        Map<String, Map<String, Object>> allFieldWiseData = fieldService.getAllProjectsFieldWiseData(portfolioId);

        for (String projectId : projectIds) {
            if (seenProjectIds.contains(projectId)) continue; // skip duplicates
            seenProjectIds.add(projectId);

            Project project = projectService.getProject(projectId);

            Optional<Portfolio> portfolio1Opt = portfolioRepo.findById(projectId);
            Portfolio portfolio1 = null;
            if (portfolio1Opt.isPresent()) {
                portfolio1 = portfolio1Opt.get();
            }

            Map<String, Object> projectProgress = new HashMap<>();

            if (project != null){
                int totalTasks = 0;
                int completedTasks = 0;

                for (Section section : project.getSection()) {
                    for (String taskId : section.getTaskIds()) {
                        Task task = taskService.getTask(taskId);
                        if (task != null) {
                            totalTasks++;
                            if (task.isCompleted()) {
                                completedTasks++;
                            }
                        }
                    }
                }

                projectProgress.put("projectId", projectId);
                projectProgress.put("projectName", project.getProjectName());
                projectProgress.put("totalTasks", totalTasks);
                projectProgress.put("completedTasks", completedTasks);
                if(project.getStartDate() != null) projectProgress.put("startDate", project.getStartDate());
                if(project.getEndDate() != null) projectProgress.put("endDate", project.getEndDate());
                projectProgress.put("priority",project.getPortfolioPriority());
                HashMap<String,String> projectOwner = new HashMap<>();
                User user = userService.getUser(project.getUserEmailid());
                if (user != null) {
                    projectOwner.put("name", user.getFullName());
                    projectOwner.put("email", user.getEmailId());
                } else {
                    projectOwner.put("name", "Unknown");
                    projectOwner.put("email", project.getUserEmailid());
                }
                projectProgress.put("projectOwner",projectOwner);

                Date startDate = project.getStartDate();
                Date endDate = project.getEndDate();

                String status;
                if (startDate != null && endDate != null) {
                    LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    LocalDate currentDate = LocalDate.now();

                    if ((currentDate.isEqual(startLocalDate) || currentDate.isAfter(startLocalDate)) &&
                            (currentDate.isEqual(endLocalDate) || currentDate.isBefore(endLocalDate))) {
                        status = "On Track";
                    } else if (currentDate.isBefore(startLocalDate)) {
                        status = "Not Started";
                    } else {
                        status = "Overdue";
                    }
                } else {
                    status = "No Dates Set";
                }

                projectProgress.put("status", status);

                projectProgress.put("progressPercent", totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0);
            }
            else if(portfolio1 != null){
                projectProgress.put("projectId", projectId);
                projectProgress.put("projectName", portfolio1.getPortfolioName());
                projectProgress.put("startDate", portfolio1.getStartDate());
                projectProgress.put("endDate", portfolio1.getEndDate());
                projectProgress.put("priority",portfolio1.getPriority());

                HashMap<String,String> projectOwner = new HashMap<>();
                User user = userService.getUserById(portfolio1.getOwnerId());
                if (user != null) {
                    log.info("User {} has logged in", user.getFullName());
                    projectOwner.put("name", user.getFullName());
                    projectOwner.put("email", user.getEmailId());
                }
                log.info("Project owner {} has logged in", projectOwner);
                projectProgress.put("projectOwner",projectOwner);
                projectProgress.put("status", "No Recent Update");
            } else {
                continue;
            }

            Map<String, Object> fieldWiseData = allFieldWiseData.get(projectId);
            if (fieldWiseData != null) {
                projectProgress.putAll(fieldWiseData);
            }

            projectProgressList.add(projectProgress);
        }

        progressSummary.put("portfolioId", portfolioId);
        progressSummary.put("portfolioName", portfolio.getPortfolioName());
        progressSummary.put("projects", projectProgressList);

        return progressSummary;
    }

    public boolean updatePriority(String portfolioId, String priority, String projectId) {
        Optional<Portfolio> optionalPortfolio = portfolioRepo.findById(portfolioId);

        if(optionalPortfolio.isPresent()) {
            Portfolio portfolio = optionalPortfolio.get();
            log.info("Updating priority for portfolio " + portfolio.getPortfolioName());

            List<String> projectIds = portfolio.getProjectsIds();

            if(projectIds.contains(projectId)) {
                // Try fetching as a Portfolio first
                Optional<Portfolio> optionalInnerPortfolio = portfolioRepo.findById(projectId);
                if(optionalInnerPortfolio.isPresent()) {
                    Portfolio innerPortfolio = optionalInnerPortfolio.get();
                    innerPortfolio.setPriority(priority);
                    save(innerPortfolio);
                    return true;
                }

                // Try fetching as a Project
                Project innerProject = projectService.getProject(projectId);
                if(innerProject != null) {
                    innerProject.setPortfolioPriority(priority);
                    projectService.save(innerProject);
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    public Map<String, Object> getPortfolioDashboardData(String portfolioId) {
        Map<String, Object> dashboard = new HashMap<>();
        Optional<Portfolio> portfolioOpt = getPortfolio(portfolioId);
        if (portfolioOpt.isEmpty()) {
            dashboard.put("error", "Portfolio not found");
            return dashboard;
        }
        Portfolio portfolio = portfolioOpt.get();
        List<String> projectIds = portfolio.getProjectsIds();
        Set<String> seenProjectIds = new HashSet<>();

        int totalTasks = 0;
        int totalCompletedTasks = 0;
        int totalIncompleteTasks = 0;
        int totalOverdueTasks = 0;
        Map<String, Integer> incompleteTasksByProject = new HashMap<>();
        Map<String, Integer> projectStatusCount = new HashMap<>();
        Map<String, Integer> upcomingTasksByAssignee = new HashMap<>();
        Map<String, Integer> portfolioStatusCount = new HashMap<>();
        List<Map<String, Object>> projectStatusList = new ArrayList<>();

        for (String projectId : projectIds) {
            if (seenProjectIds.contains(projectId)) continue;
            seenProjectIds.add(projectId);
            Project project = projectService.getProject(projectId);
            if (project == null) continue;
            Map<String, Object> stats = projectService.getDashboardStats(projectId);
            int projectTotalTasks = (int) stats.getOrDefault("totalTasks", 0);
            int projectCompletedTasks = (int) stats.getOrDefault("totalCompletedTasks", 0);
            int projectIncompleteTasks = (int) stats.getOrDefault("totalIncompleteTasks", 0);
            int projectOverdueTasks = (int) stats.getOrDefault("totalOverdueTasks", 0);
            totalTasks += projectTotalTasks;
            totalCompletedTasks += projectCompletedTasks;
            totalIncompleteTasks += projectIncompleteTasks;
            totalOverdueTasks += projectOverdueTasks;
            incompleteTasksByProject.put(project.getProjectName(), projectIncompleteTasks);

            String status = "No recent updates";
            Date now = new Date();
            if (project.getStartDate() != null && project.getEndDate() != null) {
                if (!now.before(project.getStartDate()) && !now.after(project.getEndDate())) {
                    status = "On track";
                }
            }
            projectStatusCount.put(status, projectStatusCount.getOrDefault(status, 0) + 1);
            Map<String, Object> projectStatus = new HashMap<>();
            projectStatus.put("projectName", project.getProjectName());
            projectStatus.put("status", status);
            projectStatusList.add(projectStatus);
            // Upcoming tasks by assignee
            Map<String, Integer> projectUpcoming = (Map<String, Integer>) stats.getOrDefault("upcomingTasksByAssignee", new HashMap<>());
            for (Map.Entry<String, Integer> entry : projectUpcoming.entrySet()) {
                String assignee = entry.getKey();
                int count = entry.getValue();
                upcomingTasksByAssignee.put(assignee, upcomingTasksByAssignee.getOrDefault(assignee, 0) + count);
            }
        }
        // Portfolio status (for donut chart)
        portfolioStatusCount.put("No recent updates", 0);
        portfolioStatusCount.put("On track", 0);
        for (Map<String, Object> projectStatus : projectStatusList) {
            String status = (String) projectStatus.get("status");
            portfolioStatusCount.put(status, portfolioStatusCount.getOrDefault(status, 0) + 1);
        }
        dashboard.put("totalTasks", totalTasks);
        dashboard.put("totalCompletedTasks", totalCompletedTasks);
        dashboard.put("totalIncompleteTasks", totalIncompleteTasks);
        dashboard.put("totalOverdueTasks", totalOverdueTasks);
        dashboard.put("incompleteTasksByProject", incompleteTasksByProject);
        dashboard.put("projectStatusCount", projectStatusCount);
        dashboard.put("upcomingTasksByAssignee", upcomingTasksByAssignee);
        dashboard.put("portfolioStatusCount", portfolioStatusCount);
        dashboard.put("projectStatusList", projectStatusList);
        return dashboard;
    }

    public boolean updateTime(String projectId, Date startDate, Date endDate) {
        // Try as Project first
        Project project = projectService.getProject(projectId);
        if (project != null) {
            project.setStartDate(startDate);
            project.setEndDate(endDate);
            projectService.save(project);
            return true;
        }
        // Try as Portfolio
        Optional<Portfolio> portfolioOpt = portfolioRepo.findById(projectId);
        if (portfolioOpt.isPresent()) {
            Portfolio portfolio = portfolioOpt.get();
            portfolio.setStartDate(startDate);
            portfolio.setEndDate(endDate);
            save(portfolio);
            return true;
        }
        return false;
    }

    /**
     * Delete a portfolio and clean up related data
     * This method is used during user deletion to remove portfolios owned by the user
     */
    public boolean deletePortfolio(String portfolioId) {
        try {
            Optional<Portfolio> portfolioOpt = portfolioRepo.findById(portfolioId);
            if (portfolioOpt.isEmpty()) {
                log.error("Portfolio not found with id: {}", portfolioId);
                return false;
            }

            Portfolio portfolio = portfolioOpt.get();
            log.info("Starting deletion of portfolio: {} (ID: {})", portfolio.getPortfolioName(), portfolioId);

            // Remove portfolio from other portfolios that might contain it
            removePortfolioFromOtherPortfolios(portfolioId);

            // Delete the portfolio
            portfolioRepo.delete(portfolio);
            log.info("Successfully deleted portfolio: {}", portfolio.getPortfolioName());
            
            return true;

        } catch (Exception e) {
            log.error("Error deleting portfolio {}: {}", portfolioId, e.getMessage());
            return false;
        }
    }

    /**
     * Remove portfolio from other portfolios that might contain it as a project
     */
    private void removePortfolioFromOtherPortfolios(String portfolioId) {
        try {
            List<Portfolio> allPortfolios = portfolioRepo.findAll();
            
            for (Portfolio portfolio : allPortfolios) {
                if (portfolio.getProjectsIds() != null && portfolio.getProjectsIds().contains(portfolioId)) {
                    portfolio.getProjectsIds().remove(portfolioId);
                    
                    // Also remove from attributes if present
                    if (portfolio.getAttributes() != null) {
                        portfolio.getAttributes().remove(portfolioId);
                    }
                    
                    save(portfolio);
                    log.info("Removed portfolio {} from portfolio: {}", portfolioId, portfolio.getPortfolioName());
                }
            }
            
        } catch (Exception e) {
            log.error("Error removing portfolio {} from other portfolios: {}", portfolioId, e.getMessage());
        }
    }

}
