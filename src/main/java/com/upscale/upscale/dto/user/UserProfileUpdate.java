package com.upscale.upscale.dto.user;

import lombok.Data;

@Data
public class UserProfileUpdate {

    private String fullName;
    private String pronouns;
    private String jobTitle;
    private String departmentOrTeam;
    private String role;
    private String aboutMe;

}
