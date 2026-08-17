package com.airbridge.service.impl;

import com.airbridge.dto.ChatDTO;
import com.airbridge.dto.MessageDTO;
import com.airbridge.model.Chat;
import com.airbridge.model.Message;
import com.airbridge.model.User;
import com.airbridge.repository.ChatRepository;
import com.airbridge.repository.MessageRepository;
import com.airbridge.service.ChatService;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    @Override
    @Transactional
    public ChatDTO getOrCreateChat(User currentUser, Long otherUserId) {
        User other = userService.findByIdOrThrow(otherUserId);
        Chat chat = chatRepository.findBetween(currentUser, other)
                .orElseGet(() -> chatRepository.save(Chat.builder()
                        .userOne(currentUser)
                        .userTwo(other)
                        .build()));
        return toDTO(chat, currentUser);
    }

    @Override
    public ChatDTO getChatDTO(User currentUser, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        return toDTO(chat, currentUser);
    }

    @Override
    public List<ChatDTO> getUserChats(User currentUser) {
        return chatRepository.findAllByUser(currentUser).stream()
                .map(c -> toDTO(c, currentUser))
                .toList();
    }

    @Override
    @Transactional
    public List<MessageDTO> getChatMessages(User currentUser, Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        markAsRead(currentUser, chatId);
        return messageRepository.findByChatOrderBySentAtAsc(chat).stream()
                .map(this::toMessageDTO)
                .toList();
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(User sender, Long chatId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        Message message = messageRepository.save(Message.builder()
                .chat(chat)
                .sender(sender)
                .content(content)
                .build());
        chat.setLastMessageAt(LocalDateTime.now());
        chatRepository.save(chat);
        return toMessageDTO(message);
    }

    @Override
    @Transactional
    public void markAsRead(User currentUser, Long chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow();
        messageRepository.findByChatOrderBySentAtAsc(chat).stream()
                .filter(m -> !m.getSender().getId().equals(currentUser.getId()) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
    }

    private ChatDTO toDTO(Chat chat, User currentUser) {
        User other = chat.getUserOne().getId().equals(currentUser.getId())
                ? chat.getUserTwo() : chat.getUserOne();
        List<Message> messages = messageRepository.findByChatOrderBySentAtAsc(chat);
        String lastMsg = messages.isEmpty() ? null : messages.get(messages.size() - 1).getContent();
        long unread = messageRepository.countByChatAndReadFalse(chat);
        return ChatDTO.builder()
                .id(chat.getId())
                .otherUser(userService.toDTO(other))
                .lastMessage(lastMsg)
                .lastMessageAt(chat.getLastMessageAt())
                .unreadCount(unread)
                .build();
    }

    private MessageDTO toMessageDTO(Message m) {
        return MessageDTO.builder()
                .id(m.getId())
                .chatId(m.getChat().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getFullName())
                .content(m.getContent())
                .type(m.getType())
                .read(m.isRead())
                .sentAt(m.getSentAt())
                .build();
    }
}
