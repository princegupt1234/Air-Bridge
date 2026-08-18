package com.airbridge.service.impl;

import com.airbridge.dto.BluetoothDeviceDTO;
import com.airbridge.dto.BluetoothMessagePayload;
import com.airbridge.model.BluetoothDevice;
import com.airbridge.repository.BluetoothDeviceRepository;
import com.airbridge.service.BluetoothService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BluetoothServiceImpl implements BluetoothService {

    private static final int    RFCOMM_CHANNEL = 4;
    private static final Pattern MAC_PATTERN   =
            Pattern.compile("BLUETOOTHDEVICE_([0-9A-Fa-f]{12})");

    private final BluetoothDeviceRepository deviceRepository;
    private final SimpMessagingTemplate     messagingTemplate;
    private final ObjectMapper              objectMapper;

    // mac → bridge process
    private final Map<String, Process>       bridgeProcesses = new ConcurrentHashMap<>();
    // mac → stdin writer to bridge
    private final Map<String, PrintWriter>   bridgeWriters   = new ConcurrentHashMap<>();

    private Process serverProcess;

    // ── Availability ──────────────────────────────────────────────────────────

    @Override
    public boolean isBluetoothAvailable() {
        try {
            String result = runPowerShell(
                "Get-PnpDevice -Class Bluetooth | " +
                "Where-Object {$_.Status -eq 'OK' -and $_.FriendlyName -like '*Wireless Bluetooth*' " +
                "-or ($_.Status -eq 'OK' -and $_.FriendlyName -like '*Bluetooth*' " +
                "-and $_.FriendlyName -notlike '*Enumerator*' -and $_.FriendlyName -notlike '*RFCOMM*' " +
                "-and $_.FriendlyName -notlike '*LE*' -and $_.FriendlyName -notlike '*Avrcp*')} | " +
                "Select-Object -First 1 -ExpandProperty FriendlyName"
            );
            return result != null && !result.isBlank();
        } catch (Exception e) {
            log.warn("BT availability check failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Scan ──────────────────────────────────────────────────────────────────

    @Override
    public List<BluetoothDeviceDTO> scanDevices() {
        List<BluetoothDeviceDTO> result = new ArrayList<>();
        try {
            String json = runPowerShell(
                "Get-PnpDevice -Class Bluetooth | " +
                "Where-Object {$_.Status -eq 'OK' -and $_.InstanceId -like '*BLUETOOTHDEVICE*'} | " +
                "Select-Object FriendlyName, InstanceId | ConvertTo-Json -Compress"
            );
            if (json == null || json.isBlank()) return result;

            JsonNode root = objectMapper.readTree(json.trim());
            if (root.isObject()) root = objectMapper.createArrayNode().add(root);

            for (JsonNode node : root) {
                String name       = node.path("FriendlyName").asText("Unknown");
                String instanceId = node.path("InstanceId").asText("");
                String mac        = extractMac(instanceId);
                if (mac == null) continue;
                persistDevice(mac, name);
                result.add(BluetoothDeviceDTO.builder()
                        .macAddress(mac).deviceName(name)
                        .paired(true).connected(bridgeProcesses.containsKey(mac))
                        .build());
            }
        } catch (Exception e) {
            log.error("BT scan error", e);
        }
        return result;
    }

    // ── Server ────────────────────────────────────────────────────────────────

    @Override
    public void startServer() {
        if (serverProcess != null && serverProcess.isAlive()) return;
        try {
            String script = extractScript("bt_server.py");
            serverProcess = new ProcessBuilder("python", script,
                                               String.valueOf(RFCOMM_CHANNEL))
                    .redirectErrorStream(true)
                    .start();
            log.info("BT server process started on channel {}", RFCOMM_CHANNEL);
            startBridgeReader("__server__", serverProcess);
        } catch (Exception e) {
            log.error("Failed to start BT server", e);
            throw new RuntimeException("Failed to start Bluetooth server: " + e.getMessage());
        }
    }

    @Override
    public void stopServer() {
        if (serverProcess != null) {
            serverProcess.destroyForcibly();
            serverProcess = null;
            log.info("BT server stopped");
        }
    }

    // ── Client connect ────────────────────────────────────────────────────────

    @Override
    public void connect(String macAddress) {
        if (bridgeProcesses.containsKey(macAddress)) return;
        try {
            String script = extractScript("bt_client.py");
            Process proc = new ProcessBuilder("python", script,
                                              macAddress,
                                              String.valueOf(RFCOMM_CHANNEL))
                    .redirectErrorStream(true)
                    .start();
            bridgeProcesses.put(macAddress, proc);
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8), true);
            bridgeWriters.put(macAddress, writer);
            startBridgeReader(macAddress, proc);
            log.info("BT client bridge started for {}", macAddress);
        } catch (Exception e) {
            log.error("Failed to connect to {}", macAddress, e);
            throw new RuntimeException("Bluetooth connection failed: " + e.getMessage());
        }
    }

    @Override
    public void disconnect(String macAddress) {
        Process proc = bridgeProcesses.remove(macAddress);
        if (proc != null) proc.destroyForcibly();
        bridgeWriters.remove(macAddress);
        updateConnected(macAddress, false);
        log.info("Disconnected from {}", macAddress);
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    @Override
    public void sendMessage(String macAddress, String jsonPayload) {
        PrintWriter writer = bridgeWriters.get(macAddress);
        if (writer == null) throw new RuntimeException("Not connected to " + macAddress);
        writer.println(jsonPayload);
        writer.flush();
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Override
    public List<BluetoothDeviceDTO> getConnectedDevices() {
        return deviceRepository.findByConnectedTrue().stream().map(this::toDTO).toList();
    }

    @Override
    public List<BluetoothDeviceDTO> getKnownDevices() {
        return deviceRepository.findAll().stream().map(this::toDTO).toList();
    }

    // ── Bridge reader ─────────────────────────────────────────────────────────

    private void startBridgeReader(String mac, Process proc) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    handleBridgeEvent(mac, line.trim());
                }
            } catch (IOException e) {
                log.warn("Bridge reader closed for {}: {}", mac, e.getMessage());
            } finally {
                if (!"__server__".equals(mac)) {
                    bridgeProcesses.remove(mac);
                    bridgeWriters.remove(mac);
                    updateConnected(mac, false);
                }
            }
        }, "bt-bridge-" + mac);
        t.setDaemon(true);
        t.start();
    }

    private void handleBridgeEvent(String contextMac, String json) {
        try {
            JsonNode node  = objectMapper.readTree(json);
            String  event  = node.path("event").asText();
            String  mac    = node.path("mac").asText(contextMac);

            switch (event) {
                case "connected" -> {
                    updateConnected(mac, true);
                    // Register writer for server-accepted connections
                    if ("__server__".equals(contextMac) && serverProcess != null) {
                        PrintWriter w = new PrintWriter(
                                new OutputStreamWriter(serverProcess.getOutputStream(), StandardCharsets.UTF_8), true);
                        bridgeWriters.put(mac, w);
                        bridgeProcesses.put(mac, serverProcess);
                    }
                    log.info("BT connected: {}", mac);
                }
                case "message" -> {
                    String data = node.path("data").asText();
                    log.info("BT message from {}: {}", mac, data);
                    try {
                        BluetoothMessagePayload payload =
                                objectMapper.readValue(data, BluetoothMessagePayload.class);
                        messagingTemplate.convertAndSend("/topic/bluetooth/" + mac, payload);
                    } catch (Exception ex) {
                        // raw string — wrap it
                        BluetoothMessagePayload payload = BluetoothMessagePayload.builder()
                                .senderName(mac).content(data).type("TEXT")
                                .timestamp(System.currentTimeMillis()).build();
                        messagingTemplate.convertAndSend("/topic/bluetooth/" + mac, payload);
                    }
                }
                case "disconnected" -> {
                    updateConnected(mac, false);
                    log.info("BT disconnected: {}", mac);
                }
                case "error" -> log.warn("BT bridge error for {}: {}", mac, node.path("msg").asText());
                case "server_ready" -> log.info("BT server ready on channel {}", node.path("channel").asInt());
                default -> log.debug("BT bridge event: {}", json);
            }
        } catch (Exception e) {
            log.debug("Could not parse bridge event: {}", json);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extract a Python script from classpath resources to a temp file so
     * Python can execute it regardless of whether we're in a JAR or IDE.
     */
    private String extractScript(String scriptName) throws IOException {
        Path tmp = Files.createTempFile("airbridge_" + scriptName.replace(".py", ""), ".py");
        tmp.toFile().deleteOnExit();
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("bt/" + scriptName)) {
            if (in == null) throw new IOException("Script not found in classpath: bt/" + scriptName);
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        return tmp.toAbsolutePath().toString();
    }

    private String runPowerShell(String command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-NonInteractive", "-Command", command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output;
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            output = r.lines().collect(Collectors.joining("\n")).trim();
        }
        p.waitFor(10, TimeUnit.SECONDS);
        return output;
    }

    private String extractMac(String instanceId) {
        Matcher m = MAC_PATTERN.matcher(instanceId.toUpperCase());
        if (!m.find()) return null;
        String raw = m.group(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (sb.length() > 0) sb.append(':');
            sb.append(raw, i, i + 2);
        }
        return sb.toString();
    }

    private void persistDevice(String mac, String name) {
        deviceRepository.findByMacAddress(mac).orElseGet(() ->
            deviceRepository.save(BluetoothDevice.builder()
                    .macAddress(mac).deviceName(name).paired(true).build()));
    }

    private void updateConnected(String mac, boolean connected) {
        deviceRepository.findByMacAddress(mac).ifPresent(d -> {
            d.setConnected(connected);
            if (connected) d.setLastConnectedAt(LocalDateTime.now());
            deviceRepository.save(d);
        });
    }

    private BluetoothDeviceDTO toDTO(BluetoothDevice d) {
        return BluetoothDeviceDTO.builder()
                .id(d.getId()).macAddress(d.getMacAddress())
                .deviceName(d.getDeviceName()).paired(d.isPaired())
                .connected(d.isConnected()).build();
    }
}
