package com.upscale.upscale.controller.portfolio;


import com.upscale.upscale.dto.portfolio.FieldAttribute;
import com.upscale.upscale.service.portfolio.FieldService;
import com.upscale.upscale.service.portfolio.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/portfolio/list")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class PortfolioListFieldController {

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private FieldService fieldService;

    @PostMapping("/{portfolio-id}/add-data/{field-id}")
    public ResponseEntity<?> addField(@PathVariable("portfolio-id") String portfolioId, @PathVariable("field-id") String fieldId, @RequestBody FieldAttribute fieldAttribute) {

        try{

            HashMap<String,Object> response = new HashMap<>();

            if(fieldService.updateFieldsData(fieldId,fieldAttribute,portfolioId)){
                response.put("status","success");
                response.put("message","Successfully added field");
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("status","failed");
                response.put("message","Failed to add field");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }


        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
