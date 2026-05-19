package com.chat.dto;

import lombok.Data;

@Data
public class ChatMessage {
    private String type;
    private String content;
    private String sender;
    private String roomNumber;
    private String timestamp;
    private String sessionId;
}
