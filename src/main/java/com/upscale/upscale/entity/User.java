package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
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
    private List<String> workspaces;
    private List<String> asanaUsed;
    private List<Inbox> inbox = new ArrayList<>();
    private List<Project> projects = new ArrayList<>();
    private List<String> teammates = new ArrayList<>();
   // private HashMap<String,Chat> chats = new HashMap<>();

}
