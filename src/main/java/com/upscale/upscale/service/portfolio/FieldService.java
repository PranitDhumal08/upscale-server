package com.upscale.upscale.service.portfolio;


import com.upscale.upscale.dto.portfolio.FieldData;
import com.upscale.upscale.dto.portfolio.FieldRequest;
import com.upscale.upscale.entity.portfolio.Fields;
import com.upscale.upscale.entity.portfolio.Portfolio;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.FieldsRepo;
import com.upscale.upscale.service.UserService;
import com.upscale.upscale.service.project.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class FieldService {

    @Autowired
    private FieldsRepo fieldsRepo;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    public void save(Fields fields) {
       fieldsRepo.save(fields);
    }

    public boolean addFieldNameInPortfolio(String id, String fieldName, String projectId) {

        Project project = projectService.getProject(projectId);

        if(project != null){

            //project.
        }

        Optional<Portfolio> portfolio = portfolioService.getPortfolio(projectId);

        if(portfolio.isPresent()){
            Portfolio portfolioEntity = portfolio.get();

            HashMap<String, String> fields = portfolioEntity.getFields();

            if(!fields.containsKey(id)){
                fields.put(id, fieldName);
                portfolioEntity.setFields(fields);
                portfolioService.save(portfolioEntity);
                return true;
            }
        }
        return false;
    }

    public boolean createSingleSelectField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = fieldsRepo.findByProjectId(projectId);

        if(newField == null) {
            newField = new Fields();
            newField.setProjectId(projectId);
        }

        List<FieldData> fields = newField.getFields();

        FieldData newData = new FieldData();


        if(!fieldRequest.getTitleName().isEmpty()){
            newData.setTitleName(fieldRequest.getTitleName());
            log.info("New title name set to {}", fieldName);
        }
        else{
            return false;
        }
        if(!fieldRequest.getDescription().isEmpty()){
            newData.setDescription(fieldName);
            log.info("New description set to {}", fieldName);
        }

        newData.setFieldType(fieldName);

        if(!fieldRequest.getOptions().isEmpty()){
            newData.setOptions(fieldRequest.getOptions());
            log.info("New options set to {}", fieldName);
        }
        else{
            log.error("New options not set to {}", fieldName);
            return false;
        }

        fields.add(newData);
        newField.setFields(fields);
        newField.setProjectId(projectId);
        fieldsRepo.save(newField);

        Fields fieldData = fieldsRepo.findByProjectId(projectId);
        addFieldNameInPortfolio(fieldData.getId(), fieldName, projectId);

        log.info("New field set to {}", fieldName);
        return true;
    }

    public boolean createMultiSelectField(FieldRequest fieldRequest, String projectId,String fieldName) {

        Fields newField = fieldsRepo.findByProjectId(projectId);

        if(newField == null) {
            newField = new Fields();
            newField.setProjectId(projectId);
        }
        List<FieldData> fields = newField.getFields();

        FieldData newData = new FieldData();
        if(!fieldRequest.getTitleName().isEmpty()){
            newData.setTitleName(fieldRequest.getTitleName());
            log.info("New title name set to {}", fieldRequest.getTitleName());
        }
        else{
            return false;
        }

        newData.setFieldType(fieldName);

        if(!fieldRequest.getDescription().isEmpty()){
            newData.setDescription(fieldName);
            log.info("New description set to {}", fieldName);
        }

        if(!fieldRequest.getOptions().isEmpty()){
            newData.setOptions(fieldRequest.getOptions());
            log.info("New options set to {}", fieldName);
        }
        else{
            log.error("New options not set to {}", fieldName);
            return false;
        }
        fields.add(newData);
        newField.setFields(fields);

        fieldsRepo.save(newField);
        log.info("New field set to {}", fieldName);

        Fields fieldData = fieldsRepo.findByProjectId(projectId);
        addFieldNameInPortfolio(fieldData.getId(), fieldName, projectId);

        return true;

    }

    public boolean createDateField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = fieldsRepo.findByProjectId(projectId);
        if(newField == null) {
            newField = new Fields();
            newField.setProjectId(projectId);
        }
        List<FieldData> fields = newField.getFields();
        FieldData newData = new FieldData();


        if(!fieldRequest.getTitleName().isEmpty()){
            newData.setTitleName(fieldRequest.getTitleName());
            log.info("New title name set to {}", fieldRequest.getTitleName());
        }else{
            log.error("New title name not set to {}", fieldRequest.getTitleName());
            return false;

        }

        newData.setFieldType(fieldName);
        if(!fieldRequest.getDescription().isEmpty()){
            newData.setDescription(fieldRequest.getDescription());
            log.info("New description set to {}", fieldRequest.getDescription());
        }

        if(fieldRequest.getDate() != null){
            newData.setDate(fieldRequest.getDate());
            log.info("New date set to {}", fieldRequest.getDate());
        }
        else{
            log.error("New date not set to {}", fieldRequest.getDate());
            return false;
        }

        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        log.info("New field set to {}", fieldName);

        Fields fieldData = fieldsRepo.findByProjectId(projectId);
        addFieldNameInPortfolio(fieldData.getId(), fieldName, projectId);

        return true;
    }

    public boolean createPeopleField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = fieldsRepo.findByProjectId(projectId);
        if(newField == null) {
            newField = new Fields();
            newField.setProjectId(projectId);
        }
        List<FieldData> fields = newField.getFields();
        FieldData newData = new FieldData();
        if(!fieldRequest.getTitleName().isEmpty()){
            newData.setTitleName(fieldRequest.getTitleName());
            log.info("New title name set to {}", fieldRequest.getTitleName());
        }
        else{
            log.error("New title name not set to {}", fieldRequest.getTitleName());
            return false;
        }

        newData.setFieldType(fieldName);

        if(!fieldRequest.getDescription().isEmpty()){
            newData.setDescription(fieldRequest.getDescription());
            log.info("New description set to {}", fieldRequest.getDescription());
        }

        if(!fieldRequest.getPeopleIds().isEmpty()){
            newData.setPeopleIds(fieldRequest.getPeopleIds());
            log.info("New peopleIds set to {}", fieldRequest.getPeopleIds());
        }
        else{
            log.error("New peopleIds not set to {}", fieldRequest.getPeopleIds());
            return false;
        }
        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        log.info("New field set to {}", fieldName);

        Fields fieldData = fieldsRepo.findByProjectId(projectId);
        addFieldNameInPortfolio(fieldData.getId(), fieldName, projectId);

        return true;
    }

    public boolean creteTextField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = fieldsRepo.findByProjectId(projectId);
        if(newField == null) {
            newField = new Fields();
            newField.setProjectId(projectId);

        }
        List<FieldData> fields = newField.getFields();
        FieldData newData = new FieldData();
        if(!fieldRequest.getTitleName().isEmpty()){
            newData.setTitleName(fieldRequest.getTitleName());
            log.info("New title name set to {}", fieldRequest.getTitleName());

        }
        else{
            log.error("New title name not set to {}", fieldRequest.getTitleName());
            return false;
        }

        newData.setFieldType(fieldName);
        if(!fieldRequest.getDescription().isEmpty()){
            newData.setDescription(fieldRequest.getDescription());
            log.info("New description set to {}", fieldRequest.getDescription());
        }

        fields.add(newData);
        newField.setFields(fields);
        newField.setProjectId(projectId);
        fieldsRepo.save(newField);
        log.info("New field set to {}", fieldName);

        Fields fieldData = fieldsRepo.findByProjectId(projectId);
        addFieldNameInPortfolio(fieldData.getId(), fieldName, projectId);

        return true;
    }

    public boolean createNumberField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = fieldsRepo.findByProjectId(projectId);
        if(newField == null) {
            newField = new Fields();
            newField.setProjectId(projectId);
        }
        List<FieldData> fields = newField.getFields();
        FieldData newData = new FieldData();
        if(!fieldRequest.getTitleName().isEmpty()){
            newData.setTitleName(fieldRequest.getTitleName());
            log.info("New title name set to {}", fieldRequest.getTitleName());
        }
        else{
            log.error("New title name not set to {}", fieldRequest.getTitleName());
            return false;
        }
        newData.setFieldType(fieldName);
        if(!fieldRequest.getDescription().isEmpty()){
            newData.setDescription(fieldRequest.getDescription());
            log.info("New description set to {}", fieldRequest.getDescription());
        }

        if(!fieldRequest.getFormat().isEmpty()){
            newData.setFormat(fieldRequest.getFormat());
            log.info("New format set to {}", fieldRequest.getFormat());
        }
        else{
            log.error("New format not set to {}", fieldRequest.getFormat());
            return false;
        }

        if(fieldRequest.getDecimalsPlace() != -1){
            newData.setDecimalsPlace(fieldRequest.getDecimalsPlace());
            log.info("New decimals place set to {}", fieldRequest.getDecimalsPlace());
        }
        else{
            log.error("New decimals place not set to {}", fieldRequest.getDecimalsPlace());
            return false;
        }

        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        log.info("New field set to {}", fieldName);

        Fields fieldData = fieldsRepo.findByProjectId(projectId);
        addFieldNameInPortfolio(fieldData.getId(), fieldName, projectId);

        return true;

    }

    public HashMap<String, Object> getFieldsData(String portfolioId) {
        HashMap<String, Object> data = new HashMap<>();

        Optional<Portfolio> portfolio = portfolioService.getPortfolio(portfolioId);
        if (portfolio.isEmpty()) return data;

        HashMap<String, String> fieldsData = portfolio.get().getFields();
        if (fieldsData.isEmpty()) return data;

        for (Map.Entry<String, String> entry : fieldsData.entrySet()) {
            String fieldId = entry.getKey();
            String fieldType = entry.getValue();

            Optional<Fields> fieldsOpt = fieldsRepo.findById(fieldId);
            if (fieldsOpt.isPresent()) {
                Fields fields = fieldsOpt.get();
                List<FieldData> fieldDataList = fields.getFields();
                List<Map<String, Object>> cleanedList = new ArrayList<>();

                for (FieldData fd : fieldDataList) {
                    Map<String, Object> cleaned = new HashMap<>();
                    cleaned.put("titleName", fd.getTitleName());
                    cleaned.put("fieldType", fd.getFieldType());
                    cleaned.put("description", fd.getDescription());

                    switch (fd.getFieldType()) {
                        case "single-select":
                        case "multi-select":
                            cleaned.put("options", fd.getOptions());
                            break;
                        case "date":
                            cleaned.put("date", fd.getDate());
                            break;
                        case "people":
                            List<String> ids = fd.getPeopleIds();
                            HashMap<String,String> people = new HashMap<>();
                            for(String id : ids){
                                User user = userService.getUserById(id);
                                people.put(user.getFullName(),user.getEmailId());
                            }
                            cleaned.put("peopleIds", people);
                            break;
                        case "text":
                            cleaned.put("text", fd.getText());
                            break;
                        case "number":
                            cleaned.put("format", fd.getFormat());
                            cleaned.put("decimalsPlace", fd.getDecimalsPlace());
                            break;
                    }
                    cleanedList.add(cleaned);
                }
                data.put(fieldId, cleanedList);
            }
        }

        return data;
    }

}
