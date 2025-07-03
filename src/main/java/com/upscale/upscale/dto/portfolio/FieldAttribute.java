package com.upscale.upscale.dto.portfolio;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class FieldAttribute {

    private String givenProjectId;

    //single-select
    String option;

    // or multi-select
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
