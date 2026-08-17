package com.airbridge.controller;

import com.airbridge.model.User;
import com.airbridge.service.ChatService;
import com.airbridge.service.NotificationService;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final UserService userService;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @GetMapping("/")
    public String showIndex() {
        return "home/index";
    }

    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        model.addAttribute("user", userService.toDTO(user));
        model.addAttribute("recentChats", chatService.getUserChats(user));
        model.addAttribute("unreadNotifications", notificationService.getUnreadCount(user));
        return "home/dashboard";
    }
}
