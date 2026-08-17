package com.airbridge.controller;

import com.airbridge.dto.ChatDTO;
import com.airbridge.model.User;
import com.airbridge.service.ChatService;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @GetMapping
    public String chatList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        model.addAttribute("chats", chatService.getUserChats(user));
        return "chat/chat-list";
    }

    @GetMapping("/new")
    public String newChat() {
        return "chat/new-chat";
    }

    @PostMapping("/start")
    public String startChat(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam Long userId) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        ChatDTO chat = chatService.getOrCreateChat(user, userId);
        return "redirect:/chat/" + chat.getId();
    }

    @GetMapping("/{chatId}")
    public String chatRoom(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long chatId,
                           Model model) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        ChatDTO chat = chatService.getChatDTO(user, chatId);
        model.addAttribute("messages", chatService.getChatMessages(user, chatId));
        model.addAttribute("chatId", chatId);
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("otherUser", chat.getOtherUser());
        return "chat/chat-room";
    }

    @PostMapping("/{chatId}/send")
    public String sendMessage(@AuthenticationPrincipal UserDetails userDetails,
                              @PathVariable Long chatId,
                              @RequestParam String content) {
        User user = userService.findByEmailOrThrow(userDetails.getUsername());
        chatService.sendMessage(user, chatId, content);
        return "redirect:/chat/" + chatId;
    }
}
