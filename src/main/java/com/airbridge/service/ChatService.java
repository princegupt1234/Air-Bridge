package com.airbridge.service;

import com.airbridge.dto.ChatDTO;
import com.airbridge.dto.MessageDTO;
import com.airbridge.model.User;

import java.util.List;

public interface ChatService {

    ChatDTO getOrCreateChat(User currentUser, Long otherUserId);

    ChatDTO getChatDTO(User currentUser, Long chatId);

    List<ChatDTO> getUserChats(User currentUser);

    List<MessageDTO> getChatMessages(User currentUser, Long chatId);

    MessageDTO sendMessage(User sender, Long chatId, String content);

    void markAsRead(User currentUser, Long chatId);
}
