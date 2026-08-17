package com.airbridge.dto;

import com.airbridge.model.Message.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long chatId;
    private Long senderId;
    private String senderName;
    private String content;
    private MessageType type;
    private boolean read;
    private LocalDateTime sentAt;
}
