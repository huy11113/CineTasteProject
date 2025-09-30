package com.cinetaste.userservice.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.cinetaste.userservice.service.FollowService; // Thêm import
import lombok.RequiredArgsConstructor;
import java.util.UUID;
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final FollowService followService; // Inject FollowService
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
}
