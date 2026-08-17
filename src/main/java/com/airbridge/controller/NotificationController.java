package com.airbridge.controller;

import com.airbridge.model.User;
import com.airbridge.service.NotificationService;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String showNotifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        notificationService.markAllRead(user);
        model.addAttribute("notifications", notificationService.getNotifications(user));
        return "notification/notifications";
    }
}
