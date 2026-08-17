package com.airbridge.controller;

import com.airbridge.model.User;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        model.addAttribute("user", userService.toDTO(user));
        return "profile/profile";
    }

    @GetMapping("/edit")
    public String showEditProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        model.addAttribute("user", userService.toDTO(user));
        return "profile/edit-profile";
    }

    @PostMapping("/edit")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phoneNumber,
                                @RequestParam(required = false) String about) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        userService.updateProfile(user, fullName, phoneNumber, about);
        return "redirect:/profile";
    }
}
