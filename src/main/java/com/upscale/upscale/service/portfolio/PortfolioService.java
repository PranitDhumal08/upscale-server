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
            projectProgress.put("progressPercent", totalTasks > 0 ? (completedTasks * 100.0 / totalTasks) : 0);

            projectProgressList.add(projectProgress);
        }

        progressSummary.put("portfolioId", portfolioId);
        progressSummary.put("portfolioName", portfolio.getPortfolioName());
        progressSummary.put("projects", projectProgressList);

        return progressSummary;
    }
}
