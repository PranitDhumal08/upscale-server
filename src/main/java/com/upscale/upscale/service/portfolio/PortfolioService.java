package com.upscale.upscale.service.portfolio;

import com.upscale.upscale.dto.portfolio.CreatePortFolio;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.PortfolioRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.project.ProjectService;
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
    private ProjectService projectService;

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
            portfolioIds.add(portfolioId);
            mainPortfolio.setProjectsIds(portfolioIds);
            save(mainPortfolio);

            return mainPortfolio.getPortfolioName();
        }
        return null;
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

        for (String projectId : projectIds) {
            if (seenProjectIds.contains(projectId)) continue; // skip duplicates
            seenProjectIds.add(projectId);

            Project project = projectService.getProject(projectId);
            if (project == null) continue;

            int totalTasks = 0;
            int completedTasks = 0;

            for (Section section : project.getSection()) {
                for (Task task : section.getTasks()) {
                    totalTasks++;
                    if (task.isCompleted()) {
                        completedTasks++;
                    }
                }
            }

            Map<String, Object> projectProgress = new HashMap<>();
            projectProgress.put("projectId", projectId);
            projectProgress.put("projectName", project.getProjectName());
            projectProgress.put("totalTasks", totalTasks);
            projectProgress.put("completedTasks", completedTasks);
            projectProgress.put("startDate", project.getStartDate());
            projectProgress.put("endDate", project.getEndDate());
            projectProgress.put("priority",project.getPortfolioPriority());
            HashMap<String,String> projectOwner = new HashMap<>();
            projectOwner.put("name", userService.getUser(project.getUserEmailid()).getFullName());
            projectOwner.put("email", project.getUserEmailid());

            projectProgress.put("projectOwner",projectOwner);

            Date startDate = project.getStartDate();
            Date endDate = project.getEndDate();


            LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate currentDate = LocalDate.now();

            String status;
            if ((currentDate.isEqual(startLocalDate) || currentDate.isAfter(startLocalDate)) &&
                    (currentDate.isEqual(endLocalDate) || currentDate.isBefore(endLocalDate))) {
                status = "On Track";
            } else {
                status = "No Recent Update";
            }

            projectProgress.put("status", status);

            projectProgress.put("progressPercent", totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0);

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

}
