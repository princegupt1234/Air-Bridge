package com.airbridge.service;

import com.airbridge.dto.BluetoothDeviceDTO;

import java.util.List;

public interface BluetoothService {

    /** Scan for nearby Bluetooth devices (blocking ~10s). */
    List<BluetoothDeviceDTO> scanDevices();

    /** Open RFCOMM connection to a remote device by MAC address. */
    void connect(String macAddress);

    /** Disconnect from a specific device. */
    void disconnect(String macAddress);

    /** Send a raw JSON message string to a connected device. */
    void sendMessage(String macAddress, String jsonPayload);

    /** Start the RFCOMM server socket to accept incoming connections. */
    void startServer();

    /** Stop the server socket. */
    void stopServer();

    /** Returns all devices currently connected via RFCOMM. */
    List<BluetoothDeviceDTO> getConnectedDevices();

    /** Returns all previously paired/discovered devices from DB. */
    List<BluetoothDeviceDTO> getKnownDevices();

    boolean isBluetoothAvailable();
}
