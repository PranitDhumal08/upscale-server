package com.upscale.upscale.controller.portfolio;

import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio/timeline")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class PortfolioTimelineController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserService userService;

    @GetMapping("/get/{portfolio-id}")
    public ResponseEntity<?> getPortfolio(HttpServletRequest request, @PathVariable("portfolio-id") String portfolioId) {
        try {

            String email = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            HashMap<String, List<String>> data = userService.getProjects(email);

            if(!data.isEmpty()){
                response.put("Data", data);
                log.info("Data found");

            }
            else{
                response.put("Data", "No data found");
                log.info("No Data found");

            }

            response.put("progress",portfolioService.getPortfolioTaskProgress(portfolioId));

            return new ResponseEntity<>(response, HttpStatus.OK);
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }
}
