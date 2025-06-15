package com.petfoodstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorDTO {
    private Long userId;
    private String userName;
    private String roomId;
    private Boolean isTyping;
}