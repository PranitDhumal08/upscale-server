package com.upscale.upscale.entity.workspace;

import com.upscale.upscale.service.Workspace.CuratedWork;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
@Document(collection = "workspace")
public class Workspace {

    @Id
    private String id;
    private String userId;
    private String name;
    private String description;
    private List<String> members = new ArrayList<>();

    private List<String> knowledgeId = new ArrayList<>();


    private HashMap<String, List<CuratedWork>> CuratedWorkData = new HashMap<>(); //current workspace


}
