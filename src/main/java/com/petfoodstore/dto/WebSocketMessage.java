package com.petfoodstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // MESSAGE, NOTIFICATION, TYPING, USER_STATUS, etc.
    private Object payload;
    private String roomId;
    private Long userId;
    private String timestamp;

    public WebSocketMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    public WebSocketMessage(String type, Object payload, String roomId) {
        this.type = type;
        this.payload = payload;
        this.roomId = roomId;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }
}