package com.cinetaste.userservice.controller;

import com.cinetaste.userservice.dto.UserBasicInfoResponse;
import com.cinetaste.userservice.dto.UserProfileResponse; // THÊM IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.cinetaste.userservice.service.FollowService;
import com.cinetaste.userservice.dto.UpdateProfileRequest;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import com.cinetaste.userservice.dto.FlavorProfileRequest;
import com.cinetaste.userservice.service.FlavorProfileService;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final FollowService followService;
    private final UserService userService;
    private final FlavorProfileService flavorProfileService;

    // --- ENDPOINT MỚI: Lấy thông tin chi tiết của chính mình ---
    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> getMyFullProfile(
            @RequestHeader("X-User-ID") String currentUserIdHeader) {

        UUID currentUserId = UUID.fromString(currentUserIdHeader);
        UserProfileResponse profile = userService.getUserProfileById(currentUserId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("No user authenticated");
        }
        return ResponseEntity.ok(authentication.getPrincipal());
    }

    @PostMapping("/{userIdToFollow}/follow")
    public ResponseEntity<Void> followUser(
            @PathVariable UUID userIdToFollow,
            @RequestHeader("X-User-ID") String currentUserIdHeader) {

        UUID currentUserId = UUID.fromString(currentUserIdHeader);
        followService.followUser(currentUserId, userIdToFollow);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userIdToUnfollow}/follow")
    public ResponseEntity<Void> unfollowUser(
            @PathVariable UUID userIdToUnfollow,
            @RequestHeader("X-User-ID") String currentUserIdHeader) {

        UUID currentUserId = UUID.fromString(currentUserIdHeader);
        followService.unfollowUser(currentUserId, userIdToUnfollow);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable String username) {
        try {
            UserProfileResponse userProfile = userService.getUserProfileByUsername(username);
            return ResponseEntity.ok(userProfile);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUserProfile(
            @RequestHeader("X-User-ID") String currentUserIdHeader,
            @RequestBody UpdateProfileRequest request) {

        UUID currentUserId = UUID.fromString(currentUserIdHeader);
        User updatedUser = userService.updateUserProfile(currentUserId, request);
        return ResponseEntity.ok(updatedUser);
    }

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

    @GetMapping("/{userId}/basic-info")
    public ResponseEntity<UserBasicInfoResponse> getUserBasicInfo(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserBasicInfo(userId));
    }
}