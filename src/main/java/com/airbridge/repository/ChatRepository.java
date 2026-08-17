package com.airbridge.repository;

import com.airbridge.model.Chat;
import com.airbridge.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c FROM Chat c WHERE (c.userOne = :a AND c.userTwo = :b) OR (c.userOne = :b AND c.userTwo = :a)")
    Optional<Chat> findBetween(@Param("a") User a, @Param("b") User b);

    @Query("SELECT c FROM Chat c WHERE c.userOne = :user OR c.userTwo = :user ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Chat> findAllByUser(@Param("user") User user);
}
