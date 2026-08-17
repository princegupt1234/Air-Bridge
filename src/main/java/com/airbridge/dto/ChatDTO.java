package com.airbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatDTO {
    private Long id;
    private UserDTO otherUser;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
}
