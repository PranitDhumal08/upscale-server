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
import java.util.HashMap;
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

            List<String> allMembers = peopleInvite.getReceiverEmailId();

            for(String userEmailId : allMembers){
                User user = userService.getUser(userEmailId);
                if(user != null){

                    if(!members.contains(user.getId())){
                        members.add(user.getId());

                        workspace.setMembers(members);
                        workspaceService.save(workspace);

                        log.info("User " + user.getId() + " has been saved");
                    }

                }

                for(int i = 0; i < peopleInvite.getProjectId().size(); i++){
                    Project project = projectService.getProject(peopleInvite.getProjectId().get(i));
                    Optional<Portfolio> portfolio = portfolioService.getPortfolio(peopleInvite.getProjectId().get(i));
                    if (project != null) {
                        // Determine the role for the invited user
                        String assignedRole;
                        if (peopleInvite.getRole() != null && !peopleInvite.getRole().trim().isEmpty()) {
                            // Use the role specified in the invitation
                            assignedRole = peopleInvite.getRole().trim();
                        } else if (user.getRole() != null && !user.getRole().trim().isEmpty()) {
                            // Use the user's profile role
                            assignedRole = user.getRole();
                        } else {
                            // Default role
                            assignedRole = "Team Member";
                        }

                        // Add user to teammates HashMap
                        String[] teammateInfo = {
                                userEmailId,
                            assignedRole,    // Role in project
                            "employee",      // Position (invited users are always employees)
                            user.getFullName()      // fullname
                        };
                        
                        // Initialize teammates HashMap if it's null
                        if (project.getTeammates() == null) {
                            project.setTeammates(new HashMap<>());
                        }
                        HashMap<String,String[]> teammates = project.getTeammates();
                        teammates.put(user.getId(), teammateInfo);
                        project.setTeammates(teammates);
                        //project.getTeammates().put(user.getFullName(), teammateInfo);
                        log.info("Teammate {} ({}) added to project {} with role: {}", 
                                user.getFullName(), user.getId(), project.getProjectName(), assignedRole);

                        // Also reflect membership in the user's projects list for visibility
                        try {
                            if (user.getProjects() == null) {
                                user.setProjects(new java.util.ArrayList<>());
                            }
                            java.util.List<String> projList = user.getProjects();
                            if (!projList.contains(project.getId())) {
                                projList.add(project.getId());
                                user.setProjects(projList);
                                userService.save(user);
                                log.info("Added project {} to user {} projects list during invite", project.getId(), user.getId());
                            }
                        } catch (Exception ex) {
                            log.warn("Could not add project {} to user {} during invite: {}", project.getId(), user.getId(), ex.getMessage());
                        }

                    } else if (portfolio.isPresent()) {
                        portfolio.get().getTeammates().add(user.getId());
                        log.info("Teammate " + user.getId() + " has been saved to portfolio");
                    } else {
                        log.warn("Project not found for ID: " + peopleInvite.getProjectId().get(i));
                    }

                    if(project != null) projectService.save(project);
                    else if(portfolio.isPresent()) portfolioService.save(portfolio.get());
                }

                people.setReceveriedEmailId(userEmailId);
                people.setProjectId(peopleInvite.getProjectId());
                people.setProjectsName(projectName);


                save(people);

                sendInvite(emailId, userEmailId, people);
            }
            return true;
        }
        return false;
    }

    public void sendInvite(String senderEmailId, String receiverEmailId, People people){

        inboxService.sendInviteInbox(senderEmailId,receiverEmailId, people );
    }

}
