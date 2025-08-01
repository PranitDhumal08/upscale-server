package com.upscale.upscale.entity.user;

import com.upscale.upscale.entity.project.Inbox;
import com.upscale.upscale.entity.project.Project;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
public class User {

    @Id
    private String id;
    private String emailId;
    private boolean newUser;
    private String otp;
    private String fullName;
    private String password;
    private String role;
    private String workspaces; //workspace id
    private List<String> asanaUsed;
    private List<Inbox> inbox = new ArrayList<>();
    private List<String> projects = new ArrayList<>();
    private List<String> teammates = new ArrayList<>();

   // private HashMap<String,Chat> chats = new HashMap<>();
    private String pronouns;
    private String jobTitle;
    private String departmentOrTeam;
    private String aboutMe;

}
