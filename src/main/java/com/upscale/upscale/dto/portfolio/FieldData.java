package com.upscale.upscale.dto.portfolio;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
public class FieldData {

    private String id = UUID.randomUUID().toString();
    private String titleName;
    private String fieldType;
    private String description;

    private String givenProjectId;

    //single-select or multi select
    private List<String> options = new ArrayList<>();

    //date
    private Date date;

    //people
    private List<String> peopleIds = new ArrayList<>();

    //text
    private String text;

    //number
    private String format;
    private int decimalsPlace;
}
