// Vị trí: user-service/src/main/java/com/cinetaste/userservice/service/AuthenticationService.java
package com.cinetaste.userservice.service;

import com.cinetaste.userservice.dto.LoginRequest;
import com.cinetaste.userservice.dto.LoginResponse;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        // 1. Xác thực người dùng (username/email và password)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLoginIdentifier(),
                        request.getPassword()
                )
        );

        // 2. Nếu xác thực thành công, tìm lại thông tin user
        User user = userRepository.findByUsernameOrEmail(request.getLoginIdentifier(), request.getLoginIdentifier())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or email after authentication"));

        // 3. Tạo UserDetails để sinh token
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(new ArrayList<>())
                .build();

        // 4. Sinh JWT Token
        String token = jwtService.generateToken(userDetails);

        // 5. Trả về token và thông tin người dùng
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .build();
    }
}