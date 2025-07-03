package com.upscale.upscale.entity.portfolio;

import com.upscale.upscale.dto.portfolio.FieldAttribute;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Data
@Document(collection = "portfolio")
public class Portfolio {
    @Id
    private String id;
    private String ownerId;
    private String portfolioName;
    private String privacy;
    private String defaultView;
    private List<String> teammates = new ArrayList<>();

    private List<String> projectsIds = new ArrayList<>();

    private HashMap<String, FieldAttribute> attributes = new HashMap<>();

    private String priority;

    private Date startDate;
    private Date endDate;

    private HashMap<String,String> fields = new HashMap<>();

}
