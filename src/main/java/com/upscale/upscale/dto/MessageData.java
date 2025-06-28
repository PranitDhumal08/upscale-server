package com.upscale.upscale.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MessageData {
    private String subject;
    private String body;
    private String sender;
    private List<String> recipients = new ArrayList<>();
}
