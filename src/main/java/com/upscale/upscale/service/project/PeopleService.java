package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.PeopleInvite;
import com.upscale.upscale.entity.workspace.Workspace;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.project.People;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.PeopleRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.Workspace.WorkspaceService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PeopleService {

    @Autowired
    private PeopleRepo peopleRepo;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private WorkspaceService workspaceService;


    @Autowired
    private UserService userService;
    @Autowired
    private PortfolioService portfolioService;

    public void save(People people){
        peopleRepo.save(people);
    }

    public boolean setPeople(PeopleInvite peopleInvite, String emailId){
        if(peopleInvite != null){
            People people = new People();

            List<String> projectName = new ArrayList<>();


            Workspace workspace = workspaceService.getWorkspaceByUserEmailId(emailId);

            List<String> members = workspace.getMembers();

            User user = userService.getUser(peopleInvite.getReceiverEmailId());
            if(user != null){
                members.add(user.getId());

                workspace.setMembers(members);
                workspaceService.save(workspace);

                log.info("User " + user.getId() + " has been saved");
            }

            for(int i = 0; i < peopleInvite.getProjectId().size(); i++){
                Project project = projectService.getProject(peopleInvite.getProjectId().get(i));
                Optional<Portfolio> portfolio = portfolioService.getPortfolio(peopleInvite.getProjectId().get(i));
                if (project != null) {
                   project.getTeammates().add(user.getId());
                   log.info("Teammate " + user.getId() + " has been saved");


                } else if (portfolio.isPresent()) {
                    portfolio.get().getTeammates().add(user.getId());
                    log.info("Teammate " + user.getId() + " has been saved");
                } else {
                    log.warn("Project not found for ID: " + peopleInvite.getProjectId().get(i));
                }

                if(project != null) projectService.save(project);
                else if(portfolio.isPresent()) portfolioService.save(portfolio.get());
            }

            people.setReceveriedEmailId(peopleInvite.getReceiverEmailId());
            people.setProjectId(peopleInvite.getProjectId());
            people.setProjectsName(projectName);


            save(people);

            sendInvite(emailId, peopleInvite.getReceiverEmailId(), people);
            return true;
        }
        return false;
    }

    public void sendInvite(String senderEmailId, String receiverEmailId, People people){

        inboxService.sendInviteInbox(senderEmailId,receiverEmailId, people );
    }

}
