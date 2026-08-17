package com.airbridge.service;

import com.airbridge.dto.UserDTO;
import com.airbridge.model.User;

import java.util.Optional;

public interface UserService {

    UserDTO toDTO(User user);

    Optional<User> findByEmail(String email);

    User findByEmailOrThrow(String email);

    User findByIdOrThrow(Long id);

    void updateProfile(User user, String fullName, String phoneNumber, String about);
}
