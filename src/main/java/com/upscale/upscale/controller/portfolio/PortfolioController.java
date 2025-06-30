package com.upscale.upscale.controller.portfolio;

import com.upscale.upscale.dto.portfolio.CreatePortFolio;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class PortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    @Lazy
    private UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> createPortfolio(HttpServletRequest request, @RequestBody CreatePortFolio createPortFolio) {

        try {
            String email = tokenService.getEmailFromToken(request);

            HashMap<String,Object> response = new HashMap<>();

            if(portfolioService.createPortfolio(email, createPortFolio)) {
                response.put("message", "Portfolio created");
                response.put("status", "success");
                return new ResponseEntity<>(response, HttpStatus.CREATED);
            }
            else{
                response.put("message", "Add perfect Data");
                response.put("status", "error");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/my-portfolio")
    public ResponseEntity<?> getMyPortfolio(HttpServletRequest request) {
        try {
            String email = tokenService.getEmailFromToken(request);

            HashMap<String,Object> response = new HashMap<>();

            List<Portfolio> portfolios = portfolioService.getPortFolio(email);

            if(!portfolios.isEmpty()){
                response.put("message", "Portfolio found");
                log.info("Portfolio found");
                response.put("status", "success");
                response.put("portfolios", portfolios);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "no portfolio found");
                response.put("status", "error");
                log.info("No portfolio found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/my-portfolio/{portfolio-id}")
    public ResponseEntity<?> getMyPortfolio(HttpServletRequest request, @PathVariable("portfolio-id") String portfolioId) {

        try {

            String email = tokenService.getEmailFromToken(request);

            HashMap<String,Object> response = new HashMap<>();

            Optional<Portfolio> portfolio = portfolioService.getPortfolio(portfolioId);
            if(!portfolio.isPresent()){
                response.put("message", "No portfolio found");
                response.put("status", "error");
                log.info("No portfolio found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
            else{
                response.put("message", "Portfolio found");
                response.put("portfolio", portfolio.get());
                response.put("status", "success");
                log.info("Portfolio found");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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
}
