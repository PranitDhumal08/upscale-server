package com.upscale.upscale.entity;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
public class Section {
    @Id
    private String id;
    private String sectionName;
    List<Task> tasks = new ArrayList<>();
}
