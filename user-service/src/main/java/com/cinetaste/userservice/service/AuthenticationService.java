package com.cinetaste.userservice.service;

import com.cinetaste.userservice.dto.LoginRequest;
import com.cinetaste.userservice.dto.LoginResponse;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        // 1. Xác thực người dùng
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLoginIdentifier(),
                        request.getPassword()
                )
        );

        // 2. Nếu xác thực thành công, tìm lại thông tin user
        User user = userRepository.findByUsernameOrEmail(request.getLoginIdentifier(), request.getLoginIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or email after authentication"));

        // 3. Sinh JWT Token từ đối tượng User (thay vì UserDetails)
        String token = jwtService.generateToken(user); // <-- THAY ĐỔI Ở ĐÂY

        // 4. Trả về token và thông tin người dùng
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .build();
    }
}