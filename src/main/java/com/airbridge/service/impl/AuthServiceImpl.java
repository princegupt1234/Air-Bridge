package com.airbridge.service.impl;

import com.airbridge.dto.RegisterRequest;
import com.airbridge.dto.UserDTO;
import com.airbridge.exception.EmailAlreadyExistsException;
import com.airbridge.exception.PasswordMismatchException;
import com.airbridge.model.User;
import com.airbridge.repository.UserRepository;
import com.airbridge.service.AuthService;
import com.airbridge.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        return userService.toDTO(saved);
    }
}
