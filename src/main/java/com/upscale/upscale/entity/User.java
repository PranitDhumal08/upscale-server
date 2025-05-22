package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "users")
@Data
public class User {

    @Id
    private String Id;
    private String emailId;
    private boolean newUser;
    private String otp;
    private String fullName;
    private String password;
    private String role;
    private List<String> workspaces;
    private List<String> asanaUsed;
    private List<Project> projects;

}
