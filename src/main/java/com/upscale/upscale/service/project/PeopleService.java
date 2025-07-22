package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.PeopleInvite;
import com.upscale.upscale.entity.Workspace;
import com.upscale.upscale.entity.project.People;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.PeopleRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.Workspace.WorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

                if (project != null) {
                    projectName.add(project.getProjectName());

                } else {
                    log.warn("Project not found for ID: " + peopleInvite.getProjectId().get(i));
                }
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
