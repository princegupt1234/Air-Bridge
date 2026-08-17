package com.airbridge.repository;

import com.airbridge.model.Contact;
import com.airbridge.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    List<Contact> findByOwnerAndBlockedFalse(User owner);

    Optional<Contact> findByOwnerAndContactUser(User owner, User contactUser);

    boolean existsByOwnerAndContactUser(User owner, User contactUser);
}
