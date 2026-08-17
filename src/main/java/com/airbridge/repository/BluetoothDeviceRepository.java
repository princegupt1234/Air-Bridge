package com.airbridge.repository;

import com.airbridge.model.BluetoothDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BluetoothDeviceRepository extends JpaRepository<BluetoothDevice, Long> {
    Optional<BluetoothDevice> findByMacAddress(String macAddress);
    List<BluetoothDevice> findByPairedTrue();
    List<BluetoothDevice> findByConnectedTrue();
}
