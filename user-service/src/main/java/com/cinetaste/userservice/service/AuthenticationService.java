package com.cinetaste.userservice.service;

import com.cinetaste.userservice.dto.LoginRequest;
import com.cinetaste.userservice.dto.LoginResponse;
import com.cinetaste.userservice.dto.OAuthRequest; // <-- THÊM IMPORT
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder; // <-- THÊM IMPORT
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // <-- THÊM IMPORT

import java.util.Map; // <-- THÊM IMPORT
import java.util.Optional; // <-- THÊM IMPORT
import java.util.UUID; // <-- THÊM IMPORT

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder; // <-- THÊM DEPENDENCY

    // Hàm login cũ giữ nguyên
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

        // 3. Sinh JWT Token từ đối tượng User
        String token = jwtService.generateToken(user);

        // 4. Trả về token và thông tin người dùng
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    // --- HÀM MỚI CHO GOOGLE LOGIN ---
    public LoginResponse loginWithGoogle(OAuthRequest request) {
        // 1. Dùng RestTemplate để gọi Google API và xác thực token
        RestTemplate restTemplate = new RestTemplate();
        String googleApiUrl = "https://www.googleapis.com/oauth2/v3/userinfo";

        // Tạo header chứa token
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(request.getToken());
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("", headers);

        // Gọi API
        Map<String, Object> userInfo;
        try {
            userInfo = restTemplate.exchange(googleApiUrl, org.springframework.http.HttpMethod.GET, entity, Map.class).getBody();
        } catch (Exception e) {
            throw new RuntimeException("Invalid Google token", e);
        }

        if (userInfo == null || userInfo.get("email") == null) {
            throw new RuntimeException("Could not fetch user info from Google");
        }

        String email = (String) userInfo.get("email");
        String displayName = (String) userInfo.get("name");
        String profileImageUrl = (String) userInfo.get("picture");

        // 2. Tìm hoặc Tạo người dùng
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            // Nếu tìm thấy, cập nhật thông tin (tên, ảnh) và đăng nhập
            user = existingUser.get();
            user.setDisplayName(displayName);
            user.setProfileImageUrl(profileImageUrl);
            userRepository.save(user);
        } else {
            // Nếu không tìm thấy, tạo người dùng mới
            User newUser = new User();
            newUser.setEmail(email);
            // Tạo username duy nhất từ email (ví dụ: "john.doe" từ "john.doe@gmail.com")
            newUser.setUsername(email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4));
            newUser.setDisplayName(displayName);
            newUser.setProfileImageUrl(profileImageUrl);
            // Tạo mật khẩu ngẫu nhiên (vì họ không đăng nhập bằng mật khẩu)
            newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setEmailVerifiedAt(java.time.Instant.now()); // Email đã được Google xác thực
            user = userRepository.save(newUser);
        }

        // 3. Tạo JWT của riêng bạn
        String token = jwtService.generateToken(user);

        // 4. Trả về LoginResponse
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
    // --- HÀM MỚI CHO FACEBOOK LOGIN ---
    public LoginResponse loginWithFacebook(OAuthRequest request) {
        // 1. Dùng RestTemplate để gọi Facebook Graph API
        RestTemplate restTemplate = new RestTemplate();
        // Lấy email, tên, và ảnh đại diện
        String fbApiUrl = "https://graph.facebook.com/v19.0/me?fields=id,name,email,picture.type(large)&access_token=" + request.getToken();

        Map<String, Object> userInfo;
        try {
            userInfo = restTemplate.getForObject(fbApiUrl, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Facebook token", e);
        }

        if (userInfo == null || userInfo.get("email") == null) {
            throw new RuntimeException("Could not fetch user info from Facebook. Email permission might be missing.");
        }

        String email = (String) userInfo.get("email");
        String displayName = (String) userInfo.get("name");

        // Lấy URL ảnh đại diện (nó nằm trong cấu trúc lồng nhau)
        String profileImageUrl = null;
        if (userInfo.containsKey("picture")) {
            try {
                Map<String, Object> pictureData = (Map<String, Object>) userInfo.get("picture");
                if (pictureData.containsKey("data")) {
                    Map<String, Object> data = (Map<String, Object>) pictureData.get("data");
                    if (data.containsKey("url")) {
                        profileImageUrl = (String) data.get("url");
                    }
                }
            } catch (Exception e) {
                // Bỏ qua nếu không lấy được ảnh
            }
        }

        // 2. Tìm hoặc Tạo người dùng (TÁCH RA THÀNH HÀM CHUNG)
        User user = findOrCreateUserByEmail(email, displayName, profileImageUrl);

        // 3. Tạo JWT của riêng bạn
        String token = jwtService.generateToken(user);

        // 4. Trả về LoginResponse
        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    // --- HÀM TIỆN ÍCH MỚI (Tái sử dụng cho cả Google và Facebook) ---
    private User findOrCreateUserByEmail(String email, String displayName, String profileImageUrl) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            // Nếu tìm thấy, cập nhật thông tin và đăng nhập
            user = existingUser.get();
            user.setDisplayName(displayName);
            if (profileImageUrl != null) {
                user.setProfileImageUrl(profileImageUrl);
            }
            userRepository.save(user);
        } else {
            // Nếu không tìm thấy, tạo người dùng mới
            User newUser = new User();
            newUser.setEmail(email);
            // Tạo username duy nhất từ email (ví dụ: "john.doe" từ "john.doe@gmail.com")
            newUser.setUsername(email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "") + "_" + UUID.randomUUID().toString().substring(0, 4));
            newUser.setDisplayName(displayName);
            newUser.setProfileImageUrl(profileImageUrl);
            // Tạo mật khẩu ngẫu nhiên (vì họ không đăng nhập bằng mật khẩu)
            newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setEmailVerifiedAt(java.time.Instant.now()); // Email đã được xác thực
            user = userRepository.save(newUser);
        }
        return user;
    }
}
