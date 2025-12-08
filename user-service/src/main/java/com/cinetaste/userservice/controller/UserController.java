package com.cinetaste.userservice.controller;


import com.cinetaste.userservice.dto.UserBasicInfoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.cinetaste.userservice.service.FollowService;
import com.cinetaste.userservice.dto.UserProfileResponse;
import com.cinetaste.userservice.dto.UpdateProfileRequest;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import com.cinetaste.userservice.dto.FlavorProfileRequest; // Thêm import này
import com.cinetaste.userservice.service.FlavorProfileService; // Thêm import này
import java.util.Map;
import java.util.UUID;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final FollowService followService; // Inject FollowService
    private final UserService userService;
    private final FlavorProfileService flavorProfileService;
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        // Nếu request có JWT token hợp lệ, Spring Security sẽ tự động
        // điền thông tin người dùng vào đối tượng 'authentication'.
        if (authentication == null) {
            return ResponseEntity.status(401).body("No user authenticated");
        }
        return ResponseEntity.ok(authentication.getPrincipal());
    }
    // --- ENDPOINT MỚI: Theo dõi một người dùng ---
    @PostMapping("/{userIdToFollow}/follow")
    public ResponseEntity<Void> followUser(
            @PathVariable UUID userIdToFollow,
            @RequestHeader("X-User-ID") String currentUserIdHeader) {

        UUID currentUserId = UUID.fromString(currentUserIdHeader);
        followService.followUser(currentUserId, userIdToFollow);
        return ResponseEntity.ok().build();
    }

    // --- ENDPOINT MỚI: Bỏ theo dõi một người dùng ---
    @DeleteMapping("/{userIdToUnfollow}/follow")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable UUID userIdToUnfollow,
            @RequestHeader("X-User-ID") String currentUserIdHeader) {

        UUID currentUserId = UUID.fromString(currentUserIdHeader);
        followService.unfollowUser(currentUserId, userIdToUnfollow);
        return ResponseEntity.ok().build();
    }
    // --- ENDPOINT MỚI: Lấy thông tin hồ sơ công khai của người dùng ---
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String username) {
        try {
            UserProfileResponse userProfile = userService.getUserProfileByUsername(username);
            return ResponseEntity.ok(userProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    // --- ENDPOINT MỚI: Cập nhật hồ sơ của người dùng hiện tại ---
    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUserProfile(
            @RequestHeader("X-User-ID") String currentUserIdHeader,
            @RequestBody UpdateProfileRequest request) {

        UUID currentUserId = UUID.fromString(currentUserIdHeader);
        User updatedUser = userService.updateUserProfile(currentUserId, request);
        return ResponseEntity.ok(updatedUser);
    }
    // --- ENDPOINT MỚI CHO HỒ SƠ HƯƠNG VỊ ---

    @GetMapping("/me/flavor-profile")
    public ResponseEntity<Map<String, Object>> getMyFlavorProfile(@RequestHeader("X-User-ID") String userIdHeader) {
        UUID userId = UUID.fromString(userIdHeader);
        Map<String, Object> profile = flavorProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me/flavor-profile")
    public ResponseEntity<Map<String, Object>> updateMyFlavorProfile(
            @RequestHeader("X-User-ID") String userIdHeader,
            @RequestBody FlavorProfileRequest request
    ) {
        UUID userId = UUID.fromString(userIdHeader);
        Map<String, Object> updatedProfile = flavorProfileService.updateProfile(userId, request.getPreferences());
        return ResponseEntity.ok(updatedProfile);
    }
    // Endpoint này dùng cho các microservice khác gọi (Internal use)
    @GetMapping("/{userId}/basic-info")
    public ResponseEntity<UserBasicInfoResponse> getUserBasicInfo(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserBasicInfo(userId));
    }

}
