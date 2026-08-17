package com.airbridge.service;

import com.airbridge.dto.RegisterRequest;
import com.airbridge.dto.UserDTO;

public interface AuthService {

    UserDTO register(RegisterRequest request);
}
