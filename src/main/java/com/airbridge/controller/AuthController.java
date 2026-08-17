package com.airbridge.controller;

import com.airbridge.dto.RegisterRequest;
import com.airbridge.exception.EmailAlreadyExistsException;
import com.airbridge.exception.PasswordMismatchException;
import com.airbridge.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String showLogin(@RequestParam(value = "error", required = false) String error,
                             @RequestParam(value = "logout", required = false) String logout,
                             Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegister(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                                   BindingResult bindingResult,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(registerRequest);
        } catch (PasswordMismatchException | EmailAlreadyExistsException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }

        model.addAttribute("successMessage", "Account created successfully. Please log in.");
        return "auth/login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "auth/forgot-password";
    }
}
