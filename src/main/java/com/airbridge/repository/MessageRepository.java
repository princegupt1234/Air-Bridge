package com.airbridge.repository;

import com.airbridge.model.Chat;
import com.airbridge.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatOrderBySentAtAsc(Chat chat);

    long countByChatAndReadFalse(Chat chat);
}
