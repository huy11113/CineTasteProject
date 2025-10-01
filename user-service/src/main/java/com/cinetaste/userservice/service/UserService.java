package com.cinetaste.userservice.service;


import com.cinetaste.userservice.dto.RegisterRequest;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.cinetaste.userservice.dto.UserProfileResponse; // Thêm import
import java.util.UUID;
import com.cinetaste.userservice.repository.FollowRepository;
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
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
    } // --- PHƯƠNG THỨC MỚI: Lấy thông tin hồ sơ người dùng ---
    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        // Lấy số liệu thống kê từ FollowRepository
        long followerCount = followRepository.countByFollowingId(user.getId());
        long followingCount = followRepository.countByFollowerId(user.getId());

        // Xây dựng đối tượng DTO để trả về
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .bio(user.getBio())
                .profileImageUrl(user.getProfileImageUrl())
                .memberSince(user.getCreatedAt())
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

}
