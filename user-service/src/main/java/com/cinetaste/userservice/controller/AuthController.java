package com.cinetaste.userservice.controller;

import com.cinetaste.userservice.dto.RegisterRequest;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User registeredUser = userService.registerNewUser(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}