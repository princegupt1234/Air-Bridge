package com.airbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BluetoothMessagePayload {
    private String senderEmail;
    private String senderName;
    private String content;
    private String type;       // TEXT / IMAGE / FILE / VOICE
    private long timestamp;
}
