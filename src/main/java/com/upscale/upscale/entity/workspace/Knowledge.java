package com.upscale.upscale.entity.workspace;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "knowledge")
public class Knowledge {

    @Id
    private String id;
    private String entryName;
    private String entryDescription;
    private String workspaceId;
}
