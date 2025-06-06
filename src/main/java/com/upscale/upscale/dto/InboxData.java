package com.upscale.upscale.dto;

import lombok.Data;

@Data
public class InboxData {

    private String senderId;
    private String receiverId;
    private String content;
}
