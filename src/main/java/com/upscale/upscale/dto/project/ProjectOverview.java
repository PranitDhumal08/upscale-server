package com.upscale.upscale.dto.project;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ProjectOverview {
    String projectDescription;
    Date startDate;
    Date endDate;
}
