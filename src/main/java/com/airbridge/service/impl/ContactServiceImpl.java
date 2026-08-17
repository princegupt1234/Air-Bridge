package com.airbridge.service.impl;

import com.airbridge.dto.UserDTO;
import com.airbridge.model.Contact;
import com.airbridge.model.User;
import com.airbridge.repository.ContactRepository;
import com.airbridge.service.ContactService;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final UserService userService;

    @Override
    @Transactional
    public void addContact(User owner, String contactEmail) {
        User contactUser = userService.findByEmailOrThrow(contactEmail);
        if (owner.getId().equals(contactUser.getId())) return;
        if (contactRepository.existsByOwnerAndContactUser(owner, contactUser)) return;

        contactRepository.save(Contact.builder()
                .owner(owner)
                .contactUser(contactUser)
                .build());
    }

    @Override
    @Transactional
    public void removeContact(User owner, Long contactId) {
        contactRepository.findById(contactId).ifPresent(c -> {
            if (c.getOwner().getId().equals(owner.getId())) {
                contactRepository.delete(c);
            }
        });
    }

    @Override
    @Transactional
    public void blockContact(User owner, Long contactId) {
        contactRepository.findById(contactId).ifPresent(c -> {
            if (c.getOwner().getId().equals(owner.getId())) {
                c.setBlocked(true);
                contactRepository.save(c);
            }
        });
    }

    @Override
    public List<UserDTO> getContacts(User owner) {
        return contactRepository.findByOwnerAndBlockedFalse(owner).stream()
                .map(c -> userService.toDTO(c.getContactUser()))
                .toList();
    }
}
