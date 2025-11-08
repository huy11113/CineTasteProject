package com.cinetaste.userservice.controller;

import com.cinetaste.userservice.dto.LoginRequest;
import com.cinetaste.userservice.dto.LoginResponse;
import com.cinetaste.userservice.dto.RegisterRequest;
import com.cinetaste.userservice.dto.OAuthRequest; // <-- THÊM IMPORT
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.service.AuthenticationService;
import com.cinetaste.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.cinetaste.userservice.dto.OAuthRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User registeredUser = userService.registerNewUser(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.login(loginRequest));
    }

    // --- ENDPOINT MỚI CHO GOOGLE LOGIN ---
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> loginWithGoogle(@Valid @RequestBody OAuthRequest request) {
        try {
            LoginResponse response = authenticationService.loginWithGoogle(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Trả về lỗi 401 (Unauthorized) nếu token Google không hợp lệ
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
    // --- ENDPOINT MỚI CHO FACEBOOK LOGIN ---
    @PostMapping("/facebook")
    public ResponseEntity<LoginResponse> loginWithFacebook(@Valid @RequestBody OAuthRequest request) {
        try {
            LoginResponse response = authenticationService.loginWithFacebook(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Trả về lỗi 401 (Unauthorized) nếu token Facebook không hợp lệ
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}