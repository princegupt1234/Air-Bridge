package com.airbridge.controller;

import com.airbridge.model.User;
import com.airbridge.service.ContactService;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final UserService userService;

    @GetMapping
    public String showContacts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        model.addAttribute("contacts", contactService.getContacts(user));
        return "contacts/contacts";
    }

    @GetMapping("/add")
    public String showAddContact() {
        return "contacts/add-contact";
    }

    @PostMapping("/add")
    public String addContact(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam String email,
                             RedirectAttributes ra) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        try {
            contactService.addContact(user, email);
            ra.addFlashAttribute("successMessage", "Contact added successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/contacts";
    }

    @PostMapping("/remove/{id}")
    public String removeContact(@AuthenticationPrincipal UserDetails userDetails,
                                @PathVariable Long id) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        contactService.removeContact(user, id);
        return "redirect:/contacts";
    }

    @PostMapping("/block/{id}")
    public String blockContact(@AuthenticationPrincipal UserDetails userDetails,
                               @PathVariable Long id) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        contactService.blockContact(user, id);
        return "redirect:/contacts";
    }
}
