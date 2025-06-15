package com.petfoodstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlineStatusDTO {
    private Long userId;
    private String userName;
    private String status; // ONLINE, OFFLINE, AWAY
    private LocalDateTime lastSeen;
}