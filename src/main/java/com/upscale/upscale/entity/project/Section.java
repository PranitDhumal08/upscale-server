package com.upscale.upscale.entity.project;


import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Data
public class Section {
    @Id
    private String id;
    private String sectionName;
    private List<Task> tasks = new ArrayList<>();
}
