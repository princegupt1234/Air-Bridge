package com.airbridge.controller;

import com.airbridge.dto.BluetoothDeviceDTO;
import com.airbridge.dto.BluetoothMessagePayload;
import com.airbridge.service.BluetoothService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/bluetooth")
@RequiredArgsConstructor
public class BluetoothController {

    private final BluetoothService bluetoothService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String bluetoothHome(Model model) {
        model.addAttribute("available", bluetoothService.isBluetoothAvailable());
        model.addAttribute("connectedDevices", bluetoothService.getConnectedDevices());
        model.addAttribute("knownDevices", bluetoothService.getKnownDevices());
        return "bluetooth/connected-devices";
    }

    @GetMapping("/scan")
    public String scanPage(Model model) {
        model.addAttribute("available", bluetoothService.isBluetoothAvailable());
        model.addAttribute("devices", List.of());
        return "bluetooth/bluetooth-scan";
    }

    @PostMapping("/scan")
    public String doScan(Model model) {
        List<BluetoothDeviceDTO> devices = bluetoothService.scanDevices();
        model.addAttribute("available", bluetoothService.isBluetoothAvailable());
        model.addAttribute("devices", devices);
        model.addAttribute("scanDone", true);
        return "bluetooth/bluetooth-scan";
    }

    @PostMapping("/connect")
    public String connect(@RequestParam String macAddress, RedirectAttributes ra) {
        try {
            bluetoothService.connect(macAddress);
            ra.addFlashAttribute("successMessage", "Connected to " + macAddress);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Connection failed: " + e.getMessage());
        }
        return "redirect:/bluetooth";
    }

    @PostMapping("/disconnect")
    public String disconnect(@RequestParam String macAddress, RedirectAttributes ra) {
        bluetoothService.disconnect(macAddress);
        ra.addFlashAttribute("successMessage", "Disconnected from " + macAddress);
        return "redirect:/bluetooth";
    }

    @GetMapping("/chat/{mac}")
    public String btChatPage(@PathVariable String mac, Model model) {
        model.addAttribute("mac", mac);
        return "bluetooth/pair-device";
    }

    @PostMapping("/send")
    public String sendMessage(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam String macAddress,
                              @RequestParam String content,
                              RedirectAttributes ra) {
        try {
            BluetoothMessagePayload payload = BluetoothMessagePayload.builder()
                    .senderEmail(userDetails.getUsername())
                    .senderName(userDetails.getUsername())
                    .content(content)
                    .type("TEXT")
                    .timestamp(System.currentTimeMillis())
                    .build();
            bluetoothService.sendMessage(macAddress, objectMapper.writeValueAsString(payload));
            ra.addFlashAttribute("successMessage", "Message sent");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Send failed: " + e.getMessage());
        }
        return "redirect:/bluetooth/chat/" + macAddress;
    }

    @PostMapping("/server/start")
    public String startServer(RedirectAttributes ra) {
        bluetoothService.startServer();
        ra.addFlashAttribute("successMessage", "Bluetooth server started — listening for connections");
        return "redirect:/bluetooth";
    }

    @PostMapping("/server/stop")
    public String stopServer(RedirectAttributes ra) {
        bluetoothService.stopServer();
        ra.addFlashAttribute("successMessage", "Bluetooth server stopped");
        return "redirect:/bluetooth";
    }
}
