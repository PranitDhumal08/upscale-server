package com.upscale.upscale.controller.portfolio;


import com.upscale.upscale.service.portfolio.FieldService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio/list")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class PortfolioListFieldController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private FieldService fieldService;

    //@GetMapping("")
}
