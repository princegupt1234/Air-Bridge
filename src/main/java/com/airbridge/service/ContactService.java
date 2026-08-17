package com.airbridge.service;

import com.airbridge.dto.UserDTO;
import com.airbridge.model.User;

import java.util.List;

public interface ContactService {

    void addContact(User owner, String contactEmail);

    void removeContact(User owner, Long contactId);

    void blockContact(User owner, Long contactId);

    List<UserDTO> getContacts(User owner);
}
