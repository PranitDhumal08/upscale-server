package com.upscale.upscale.controller.portfolio;


import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class PortfolioListController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private UserService userService;


    @PostMapping("/add-project/{portfolio-id}")
    public ResponseEntity<?> addProjectInPortfolio(HttpServletRequest request, @RequestBody HashMap<String,String> map, @PathVariable("portfolio-id") String portfolioId) {

        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            String projectId = "";
            String reqPortfolioId = "";


            if(map.get("portfolio-id") != null) reqPortfolioId = map.get("portfolio-id");
            if(map.get("project-id") != null) projectId = map.get("project-id");

            List<String> res = new ArrayList<>();
            if(!projectId.isEmpty()){

                res.add(portfolioService.addProjectToPortfolio(projectId, portfolioId));

            }

            if(!reqPortfolioId.isEmpty()){
                res.add(portfolioService.addPortfolioToPortfolio(portfolioId, reqPortfolioId));
            }

            if(res.size() > 0){
                response.put("status", "success");
                response.put("portfolios", res);
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            }
            else{
                response.put("status", "error");
                response.put("portfolioId", "error");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list/{portfolio-id}")
    public ResponseEntity<?> getPortfolioList(HttpServletRequest request, @PathVariable("portfolio-id") String portfolioId) {
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

    @PutMapping("/set-priority/{portfolio-id}/{project-id}")
    public ResponseEntity<?> setProjectPriority(HttpServletRequest request, @RequestBody HashMap<String,String> map, @PathVariable("portfolio-id") String portfolioId, @PathVariable("project-id") String projectId) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            String priority = map.get("priority");

            if(portfolioService.updatePriority(portfolioId,priority,projectId)){
                response.put("status", "success");
                response.put("message", "Project priority updated");

                log.info("Project priority updated");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }else{
                response.put("status", "error");
                response.put("message", "Project priority update failed");
                log.info("Project priority update failed");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
