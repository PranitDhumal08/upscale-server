package com.upscale.upscale.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "people")
public class People {

    @Id
    private String id;
    private List<String> projectId = new ArrayList<>();
    private String receveriedEmailId;
    private List<String> projectsName = new ArrayList<>();
}
