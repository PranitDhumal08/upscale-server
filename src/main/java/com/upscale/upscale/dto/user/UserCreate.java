package com.upscale.upscale.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class UserCreate {
    private String password;
    private String fullName;
    private String role;
    private String otp = "";
    private List<String> workspaces;
    private List<String> asanaUsed;
}
