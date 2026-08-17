package com.airbridge.service;

import com.airbridge.dto.NotificationDTO;
import com.airbridge.model.User;

import java.util.List;

public interface NotificationService {

    void send(User user, String title, String body);

    List<NotificationDTO> getNotifications(User user);

    long getUnreadCount(User user);

    void markAllRead(User user);
}
