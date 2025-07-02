package com.upscale.upscale.controller.portfolio;

import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.service.portfolio.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio/dashboard")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class PortfolioDashboardController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/get/{portfolio-id}")
    public ResponseEntity<?> getPortfolio(@PathVariable("portfolio-id") String portfolioId) {
        try {
            Map<String, Object> dashboard = portfolioService.getPortfolioDashboardData(portfolioId);
            HashMap<String, Object> response = new HashMap<>();
            if(dashboard != null) {
                response.put("message", "Success");
                response.put("dashboard", dashboard);
                return new ResponseEntity<>(response, HttpStatus.OK);

            }
            else{
                response.put("message", "Error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
