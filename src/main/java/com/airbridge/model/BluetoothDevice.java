package com.airbridge.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bluetooth_devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BluetoothDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String macAddress;

    @Column(nullable = false, length = 100)
    private String deviceName;

    @Builder.Default
    private boolean paired = false;

    @Builder.Default
    private boolean connected = false;

    @Builder.Default
    private LocalDateTime discoveredAt = LocalDateTime.now();

    private LocalDateTime lastConnectedAt;
}
