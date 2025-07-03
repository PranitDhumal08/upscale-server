package com.upscale.upscale.dto.portfolio;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class FieldRequest {

    private String titleName;
    private String description;

    //single-select or multi select
    private List<String> options = new ArrayList<>();

    //date
    Date date;

    //people
    private List<String> peopleIds = new ArrayList<>();

    //text
    //private String text;

    //number
    private String format;
    private int decimalsPlace;

    //formula


}
