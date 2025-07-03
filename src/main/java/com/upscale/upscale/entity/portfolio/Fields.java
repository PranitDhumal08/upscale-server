package com.upscale.upscale.entity.portfolio;

import com.upscale.upscale.dto.portfolio.FieldData;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "fields")
public class Fields {

    @Id
    private String id;

    private String projectId;

    private List<FieldData> fields = new ArrayList<>();

}

