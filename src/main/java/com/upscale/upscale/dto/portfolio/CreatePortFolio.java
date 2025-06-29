package com.upscale.upscale.dto.portfolio;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreatePortFolio {
    private String portfolioName;
    private String privacy;
    private String defaultView;
}
