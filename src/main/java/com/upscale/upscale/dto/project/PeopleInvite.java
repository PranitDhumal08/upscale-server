package com.upscale.upscale.dto.project;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PeopleInvite {

    private List<String> receiverEmailId;
    private List<String> projectId = new ArrayList<>();
    private String role; // Optional: specific role for the invited user (e.g., "Developer", "Designer")

}
