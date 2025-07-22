package com.upscale.upscale.service.Workspace;


import com.upscale.upscale.entity.Workspace;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.WorkspaceRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import com.upscale.upscale.service.project.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class WorkspaceService {

    @Autowired
    private WorkspaceRepo workspaceRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private PortfolioService portfolioService;

    public void save(Workspace workspace) {
        workspaceRepo.save(workspace);
    }

    public Workspace createWorkspace(String userId) {

        Workspace workspace = new Workspace();

        workspace.setName("My Workspace");
        workspace.setUserId(userId);

        List<String> members = new ArrayList<>();
        members.add(userId);
        workspace.setMembers(members);

        save(workspace);
        log.info("Created Workspace: " + workspace.getName() + " with ID: " + workspace.getUserId());


        Workspace workspace1 = workspaceRepo.findByUserId(userId);
        return workspace1;
    }

    public Workspace getWorkspace(String userId) {

        return workspaceRepo.findByUserId(userId);
    }

    public Workspace getWorkspaceByUserEmailId(String email) {
        User user = userService.getUser(email);
        return workspaceRepo.findByUserId(user.getId());
    }

    public boolean setCuratedWork(String sectionName, String userId, CuratedWork curatedWork) {
        Workspace workspace = getWorkspace(userId);

        HashMap<String,List<CuratedWork>> curatedWorkHashMap = workspace.getCuratedWorkData();

        if(curatedWorkHashMap.containsKey(sectionName)) {

            List<CuratedWork> curatedWorkList = curatedWorkHashMap.get(sectionName);

            Project project = projectService.getProject(curatedWork.getProjectId());
            Optional<Portfolio> portfolio = portfolioService.getPortfolio(curatedWork.getProjectId());
            if(project != null){
                curatedWork.setProjectName(project.getProjectName());
            }
            else if(portfolio != null && portfolio.get() != null) {
                Portfolio portfolio1 = portfolio.get();
                curatedWork.setProjectName(portfolio1.getPortfolioName());
            }

            for(CuratedWork curatedWorkData : curatedWorkList) {

                if(curatedWork.getProjectId().equals(curatedWorkData.getProjectId())) {
                    return false;
                }
            }

            curatedWorkList.add(curatedWork);

            curatedWorkHashMap.put(sectionName, curatedWorkList);

            workspace.setCuratedWorkData(curatedWorkHashMap);
            workspaceRepo.save(workspace);

            return true;
        }

        return false;
    }

    public HashMap<String,String> getMemberInfo(String userId){

        User user = userService.getUserById(userId);

        HashMap<String,String> userInfo = new HashMap<>();

        if(user != null){

            if(!user.getFullName().isEmpty())userInfo.put("Name",user.getFullName());
            else userInfo.put("Name",user.getEmailId());

            userInfo.put("jobTitle",user.getJobTitle());

            return userInfo;
        }

        return null;
    }
}
