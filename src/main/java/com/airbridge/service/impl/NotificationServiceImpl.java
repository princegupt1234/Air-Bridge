package com.airbridge.service.impl;

import com.airbridge.dto.NotificationDTO;
import com.airbridge.model.Notification;
import com.airbridge.model.User;
import com.airbridge.repository.NotificationRepository;
import com.airbridge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void send(User user, String title, String body) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .build());
    }

    @Override
    public List<NotificationDTO> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(n -> NotificationDTO.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .body(n.getBody())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Override
    @Transactional
    public void markAllRead(User user) {
        notificationRepository.findByUserOrderByCreatedAtDesc(user).forEach(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }
}
