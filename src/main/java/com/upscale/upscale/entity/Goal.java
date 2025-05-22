package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "goals")
public class Goal {
    @Id
    private String id;
    private String userId;
    private String goalTitle;
    private String goalOwner;
    private String timePeriod;
    private String privacy;
    private List<String> members = new ArrayList<>();
}
