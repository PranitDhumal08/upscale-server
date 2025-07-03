package com.upscale.upscale.service.portfolio;


import com.upscale.upscale.dto.portfolio.FieldAttribute;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class FieldService {

    @Autowired
    private FieldsRepo fieldsRepo;

    @Autowired
    @Lazy
    private PortfolioService portfolioService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    public void save(Fields fields) {
       fieldsRepo.save(fields);
    }

    public boolean addFieldNameInPortfolio(String id, String titleName, String projectId) {
        Project project = projectService.getProject(projectId);
        if(project != null){
            //project.
        }
        Optional<Portfolio> portfolio = portfolioService.getPortfolio(projectId);
        if(portfolio.isPresent()){
            Portfolio portfolioEntity = portfolio.get();
            HashMap<String, String> fields = portfolioEntity.getFields();
            if(!fields.containsKey(id)){
                fields.put(id, titleName);
                portfolioEntity.setFields(fields);
                portfolioService.save(portfolioEntity);
                return true;
            }
        }
        return false;
    }



    public boolean createSingleSelectField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = new Fields();
        newField.setProjectId(projectId);
        FieldData newData = new FieldData();
        newData.setId(UUID.randomUUID().toString());
        if (fieldRequest.getTitleName() == null || fieldRequest.getTitleName().isEmpty()) return false;
        newData.setTitleName(fieldRequest.getTitleName());
        if (fieldRequest.getDescription() != null && !fieldRequest.getDescription().isEmpty()) {
            newData.setDescription(fieldRequest.getDescription());
        }
        newData.setFieldType(fieldName);
        if (fieldRequest.getOptions() == null || fieldRequest.getOptions().isEmpty()) {
            log.error("New options not set to {}", fieldName);
            return false;
        }
        newData.setOptions(fieldRequest.getOptions());
        List<FieldData> fields = new ArrayList<>();
        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        List<Fields> fieldDataList = fieldsRepo.findAllByProjectId(projectId);
        for (Fields fieldData : fieldDataList) {
            if (!fieldData.getFields().isEmpty()) {
                addFieldNameInPortfolio(fieldData.getId(), fieldData.getFields().get(0).getTitleName(), projectId);
            }
        }
        if (!fieldDataList.isEmpty() && !fieldDataList.get(0).getFields().isEmpty()) {
            addFieldNameInPortfolio(fieldDataList.get(0).getId(), fieldDataList.get(0).getFields().get(0).getTitleName(), projectId);
        }
        return true;
    }


    public boolean createMultiSelectField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = new Fields();
        newField.setProjectId(projectId);
        FieldData newData = new FieldData();
        newData.setId(UUID.randomUUID().toString());
        if (fieldRequest.getTitleName() == null || fieldRequest.getTitleName().isEmpty()) {
            log.error("Title name is missing");
            return false;
        }
        newData.setTitleName(fieldRequest.getTitleName());
        if (fieldRequest.getDescription() != null && !fieldRequest.getDescription().isEmpty()) {
            newData.setDescription(fieldRequest.getDescription());
        }
        newData.setFieldType(fieldName);
        if (fieldRequest.getOptions() == null || fieldRequest.getOptions().isEmpty()) {
            log.error("Options not provided for {}", fieldName);
            return false;
        }
        newData.setOptions(fieldRequest.getOptions());
        List<FieldData> fields = new ArrayList<>();
        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        List<Fields> fieldDataList = fieldsRepo.findAllByProjectId(projectId);
        for (Fields fieldData : fieldDataList) {
            if (!fieldData.getFields().isEmpty()) {
                addFieldNameInPortfolio(fieldData.getId(), fieldData.getFields().get(0).getTitleName(), projectId);
            }
        }
        return true;
    }


    public boolean createDateField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = new Fields();
        newField.setProjectId(projectId);
        FieldData newData = new FieldData();
        newData.setId(UUID.randomUUID().toString());
        if (fieldRequest.getTitleName() == null || fieldRequest.getTitleName().isEmpty()) {
            log.error("Title name is missing");
            return false;
        }
        newData.setTitleName(fieldRequest.getTitleName());
        if (fieldRequest.getDescription() != null && !fieldRequest.getDescription().isEmpty()) {
            newData.setDescription(fieldRequest.getDescription());
        }
        newData.setFieldType(fieldName);
        if (fieldRequest.getDate() != null && !fieldRequest.getDate().toString().isEmpty()) {
            newData.setDate(fieldRequest.getDate());
            log.info("New date set to {}", fieldRequest.getDate());
        } else {
            newData.setDate(null);
            log.warn("Date not provided, setting to null");
        }
        List<FieldData> fields = new ArrayList<>();
        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        List<Fields> fieldDataList = fieldsRepo.findAllByProjectId(projectId);
        for (Fields fieldData : fieldDataList) {
            if (!fieldData.getFields().isEmpty()) {
                addFieldNameInPortfolio(fieldData.getId(), fieldData.getFields().get(0).getTitleName(), projectId);
            }
        }
        return true;
    }


    public boolean createPeopleField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = new Fields();
        newField.setProjectId(projectId);
        FieldData newData = new FieldData();
        newData.setId(UUID.randomUUID().toString());
        if (fieldRequest.getTitleName() == null || fieldRequest.getTitleName().isEmpty()) {
            log.error("Title name not provided");
            return false;
        }
        newData.setTitleName(fieldRequest.getTitleName());
        if (fieldRequest.getDescription() != null && !fieldRequest.getDescription().isEmpty()) {
            newData.setDescription(fieldRequest.getDescription());
        }
        newData.setFieldType(fieldName);
        if (fieldRequest.getPeopleIds() == null || fieldRequest.getPeopleIds().isEmpty()) {
            log.error("People IDs not provided");
            return false;
        }
        newData.setPeopleIds(fieldRequest.getPeopleIds());
        List<FieldData> fields = new ArrayList<>();
        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        List<Fields> fieldDataList = fieldsRepo.findAllByProjectId(projectId);
        for (Fields fieldData : fieldDataList) {
            if (!fieldData.getFields().isEmpty()) {
                addFieldNameInPortfolio(fieldData.getId(), fieldData.getFields().get(0).getTitleName(), projectId);
            }
        }
        log.info("New field set to {}", fieldName);
        return true;
    }


    public boolean createTextField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = new Fields();
        newField.setProjectId(projectId);
        FieldData newData = new FieldData();
        newData.setId(UUID.randomUUID().toString());
        if (fieldRequest.getTitleName() == null || fieldRequest.getTitleName().isEmpty()) {
            log.error("Title name not provided");
            return false;
        }
        newData.setTitleName(fieldRequest.getTitleName());
        if (fieldRequest.getDescription() != null && !fieldRequest.getDescription().isEmpty()) {
            newData.setDescription(fieldRequest.getDescription());
        }
        newData.setFieldType(fieldName);
        List<FieldData> fields = new ArrayList<>();
        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        List<Fields> fieldDataList = fieldsRepo.findAllByProjectId(projectId);
        for (Fields fieldData : fieldDataList) {
            if (!fieldData.getFields().isEmpty()) {
                addFieldNameInPortfolio(fieldData.getId(), fieldData.getFields().get(0).getTitleName(), projectId);
            }
        }
        log.info("New text field set to {}", fieldName);
        return true;
    }


    public boolean createNumberField(FieldRequest fieldRequest, String projectId, String fieldName) {
        Fields newField = new Fields();
        newField.setProjectId(projectId);
        FieldData newData = new FieldData();
        newData.setId(UUID.randomUUID().toString());
        if (fieldRequest.getTitleName() == null || fieldRequest.getTitleName().isEmpty()) {
            log.error("Title name not provided");
            return false;
        }
        newData.setTitleName(fieldRequest.getTitleName());
        if (fieldRequest.getDescription() != null && !fieldRequest.getDescription().isEmpty()) {
            newData.setDescription(fieldRequest.getDescription());
        }
        newData.setFieldType(fieldName);
        if (fieldRequest.getFormat() == null || fieldRequest.getFormat().isEmpty()) {
            log.error("Format not provided");
            return false;
        }
        newData.setFormat(fieldRequest.getFormat());
        if (fieldRequest.getDecimalsPlace() == -1) {
            log.error("Decimals place not set or invalid");
            return false;
        }
        newData.setDecimalsPlace(fieldRequest.getDecimalsPlace());
        List<FieldData> fields = new ArrayList<>();
        fields.add(newData);
        newField.setFields(fields);
        fieldsRepo.save(newField);
        List<Fields> fieldDataList = fieldsRepo.findAllByProjectId(projectId);
        for (Fields fieldData : fieldDataList) {
            if (!fieldData.getFields().isEmpty()) {
                addFieldNameInPortfolio(fieldData.getId(), fieldData.getFields().get(0).getTitleName(), projectId);
            }
        }
        log.info("New number field set to {}", fieldName);
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
                    cleaned.put("id", fd.getId());
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
                            if(ids.isEmpty()){
                                cleaned.put("peopleIds", ids);
                            }
                            else{
                                HashMap<String,String> people = new HashMap<>();
                                for(String id : ids){
                                    if (id == null || id.trim().isEmpty()) continue;
                                    User user = userService.getUserById(id);
                                    if (user != null) {
                                        people.put(user.getFullName(), user.getEmailId());
                                    } else {
                                        people.put("Unknown", id);
                                    }
                                }
                                cleaned.put("peopleIds", people);
                            }
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

    public FieldAttribute setFieldWiseAttributes(String fieldType, FieldAttribute input) {
        FieldAttribute attr = new FieldAttribute();

        switch (fieldType) {
            case "single-select":
                attr.setOption(input.getOption());
                break;
            case "multi-select":
                attr.setOptions(input.getOptions());
                break;
            case "date":
                attr.setDate(input.getDate());
                break;
            case "people":
                attr.setPeopleIds(input.getPeopleIds());
                break;
            case "text":
                attr.setText(input.getText());
                break;
            case "number":
                attr.setFormat(input.getFormat());
                attr.setDecimalsPlace(input.getDecimalsPlace());
                break;
            // add more cases as needed
        }
        return attr;
    }

    public boolean updateFieldsData(String fieldId, FieldAttribute fieldAttribute, String portfolioId) {
        Optional<Fields> fieldsOpt = fieldsRepo.findById(fieldId);
        if (fieldsOpt.isEmpty()) return false;

        Fields fieldData = fieldsOpt.get();
        List<FieldData> fieldDataList = fieldData.getFields();
        if (fieldDataList.isEmpty()) return false;

        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolio(portfolioId);
        if (portfolioOpt.isEmpty()) return false;

        Portfolio portfolio = portfolioOpt.get();
        HashMap<String, FieldAttribute> attributes = portfolio.getAttributes();
        String projectId = fieldAttribute.getGivenProjectId();
        if (projectId == null) return false;

        attributes.put(projectId, setFieldWiseAttributes(fieldDataList.get(0).getFieldType(), fieldAttribute));
        portfolio.setAttributes(attributes);
        portfolioService.save(portfolio);
        log.info("Field attribute set for project {}", projectId);
        return true;
    }

    public HashMap<String, Object> getFieldWiseDataForProject(String portfolioId, String projectId) {
        HashMap<String, Object> result = new HashMap<>();
        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolio(portfolioId);
        if (portfolioOpt.isEmpty()) return result;

        Portfolio portfolio = portfolioOpt.get();
        HashMap<String, String> fields = portfolio.getFields(); // fieldId → fieldType
        HashMap<String, FieldAttribute> attributes = portfolio.getAttributes(); // projectId → FieldAttribute

        FieldAttribute attr = attributes.get(projectId);
        if (attr == null) return result;

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String fieldId = entry.getKey();
            String fieldType = entry.getValue();

            switch (fieldType) {
                case "single-select":
                    String singleOptions = attr.getOption();
                    result.put(fieldId, singleOptions);
                    break;
                case "multi-select":
                    // Return the full list
                    result.put(fieldId, attr.getOptions());
                    break;
                case "date":
                    result.put(fieldId, attr.getDate());
                    break;
                case "people":
                    result.put(fieldId, attr.getPeopleIds());
                    break;
                case "text":
                    result.put(fieldId, attr.getText());
                    break;
                case "number":
                    Map<String, Object> numberData = new HashMap<>();
                    numberData.put("format", attr.getFormat());
                    numberData.put("decimalsPlace", attr.getDecimalsPlace());
                    result.put(fieldId, numberData);
                    break;
                // Add more cases as needed
            }
        }
        return result;
    }

    public Map<String, Map<String, Object>> getAllProjectsFieldWiseData(String portfolioId) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Optional<Portfolio> portfolioOpt = portfolioService.getPortfolio(portfolioId);
        if (portfolioOpt.isEmpty()) return result;

        Portfolio portfolio = portfolioOpt.get();
        List<String> projectIds = portfolio.getProjectsIds();
        HashMap<String, String> fieldIdToName = portfolio.getFields(); // fieldId → fieldName
        for (String projectId : projectIds) {
            Map<String, Object> fieldWiseData = getFieldWiseDataForProject(portfolioId, projectId);
            Map<String, Object> fieldNameWiseData = new HashMap<>();
            for (Map.Entry<String, Object> entry : fieldWiseData.entrySet()) {
                String fieldId = entry.getKey();
                String fieldName = fieldIdToName.get(fieldId);
                if (fieldName != null) {
                    fieldNameWiseData.put(fieldName, entry.getValue());
                }
            }
            result.put(projectId, fieldNameWiseData);
        }
        return result;
    }

}
