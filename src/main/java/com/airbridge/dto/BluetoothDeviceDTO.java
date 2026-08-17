package com.airbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BluetoothDeviceDTO {
    private Long id;
    private String macAddress;
    private String deviceName;
    private boolean paired;
    private boolean connected;
}
