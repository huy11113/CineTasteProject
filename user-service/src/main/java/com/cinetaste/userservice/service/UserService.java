package com.cinetaste.userservice.service;


import com.cinetaste.userservice.dto.RegisterRequest;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerNewUser(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalStateException("Username already taken");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("Email already registered");
        }

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // Các trường mặc định (role, is_active...) đã được thiết lập trong Entity

        return userRepository.save(newUser);
    }
}
