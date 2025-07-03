package com.upscale.upscale.controller.portfolio;


import com.upscale.upscale.dto.portfolio.FieldAttribute;
import com.upscale.upscale.dto.portfolio.FieldData;
import com.upscale.upscale.dto.portfolio.FieldRequest;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.portfolio.FieldService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.text.SimpleDateFormat;

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
    public ResponseEntity<?> getPortfolioList(HttpServletRequest request, @PathVariable("portfolio-id") String portfolioId, @RequestParam(value = "projectId", required = false) String projectId) {
        try {
            String email = tokenService.getEmailFromToken(request);
            HashMap<String,Object> response = new HashMap<>();

            response.put("fields", fieldService.getFieldsData(portfolioId));
            response.put("basicInfo", portfolioService.getBasicInfo(portfolioId));

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


//            response.put("fieldWiseData", fieldService.getAllProjectsFieldWiseData(portfolioId));
//            if (projectId != null && !projectId.isEmpty()) {
//                Optional<Portfolio> portfolioOpt = portfolioService.getPortfolio(portfolioId);
//                if (portfolioOpt.isPresent()) {
//                    Portfolio portfolio = portfolioOpt.get();
//                    HashMap<String, FieldAttribute> attributes = portfolio.getAttributes();
//                    FieldAttribute attr = attributes.get(projectId);
//                    response.put("fieldAttribute", attr); // could be null if not set
//                }
//            }

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

    @PutMapping("/set-time/{portfolio-id}/{project-id}")
    public ResponseEntity<?> setTime(@PathVariable("portfolio-id") String portfolioId, @PathVariable("project-id") String projectId, @RequestBody HashMap<String,Object> map) {
        try {
            Object startDateObj = map.get("startDate");
            Object endDateObj = map.get("endDate");
            Date startDate = null;
            Date endDate = null;
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            if (startDateObj instanceof String) {
                startDate = isoFormat.parse((String) startDateObj);
            } else if (startDateObj instanceof Date) {
                startDate = (Date) startDateObj;
            }
            if (endDateObj instanceof String) {
                endDate = isoFormat.parse((String) endDateObj);
            } else if (endDateObj instanceof Date) {
                endDate = (Date) endDateObj;
            }
            boolean updated = portfolioService.updateTime(projectId, startDate, endDate);
            HashMap<String, Object> response = new HashMap<>();
            if (updated) {
                response.put("status", "success");
                response.put("message", "Time updated successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("status", "error");
                response.put("message", "Project or Portfolio not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Autowired
    private FieldService fieldService;

    @PostMapping("/add-field/{portfolio-id}/{field-name}")
    public ResponseEntity<?> addField(@PathVariable("portfolio-id") String portfolioId, @PathVariable("field-name") String fieldName, @RequestBody FieldRequest FieldRequest) {
        try {


            HashMap<String,Object> response = new HashMap<>();

            if(fieldName.equals("single-select")){
                if(fieldService.createSingleSelectField(FieldRequest,portfolioId, fieldName)){
                    response.put("status", "success");
                    response.put("message", "Field added successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }else{
                    response.put("status", "error");
                    response.put("message", "Field added failed");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }

            else if(fieldName.equals("multi-select")){

                if(fieldService.createMultiSelectField(FieldRequest,portfolioId, fieldName)){
                    response.put("status", "success");
                    response.put("message", "Field added successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("status", "error");
                    response.put("message", "Field added failed");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else if(fieldName.equals("date")){

                if(fieldService.createDateField(FieldRequest,portfolioId, fieldName)){
                    response.put("status", "success");
                    response.put("message", "Field added successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("status", "error");
                    response.put("message", "Field added failed");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else if(fieldName.equals("people")){

                if(fieldService.createPeopleField(FieldRequest,portfolioId, fieldName)){
                    response.put("status", "success");
                    response.put("message", "Field added successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("status", "error");
                    response.put("message", "Field added failed");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else if(fieldName.equals("text")){
                if(fieldService.createTextField(FieldRequest,portfolioId, fieldName)){
                    response.put("status", "success");
                    response.put("message", "Field added successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("status", "error");
                    response.put("message", "Field added failed");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else if(fieldName.equals("number")){

                if(fieldService.createNumberField(FieldRequest,portfolioId, fieldName)){
                    response.put("status", "success");
                    response.put("message", "Field added successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("status", "error");
                    response.put("message", "Field added failed");
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            else{
                response.put("status", "error");
                response.put("message", "check field type");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
