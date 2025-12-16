package com.cinetaste.userservice.service;

import com.cinetaste.userservice.entity.Follow;
import com.cinetaste.userservice.entity.FollowId;
import com.cinetaste.userservice.entity.User;
import com.cinetaste.userservice.repository.FollowRepository;
import com.cinetaste.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    @Transactional
    public void followUser(UUID currentUserId, UUID userToFollowId) {
        if (currentUserId.equals(userToFollowId)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found."));
        User userToFollow = userRepository.findById(userToFollowId)
                .orElseThrow(() -> new RuntimeException("User to follow not found."));

        FollowId followId = new FollowId();
        followId.setFollowerId(currentUserId);
        followId.setFollowingId(userToFollowId);

        if (followRepository.existsById(followId)) {
            throw new IllegalStateException("You are already following this user.");
        }

        Follow newFollow = new Follow();
        newFollow.setId(followId);
        newFollow.setFollower(currentUser);
        newFollow.setFollowing(userToFollow);

        followRepository.save(newFollow);
    }

    @Transactional
    public void unfollowUser(UUID currentUserId, UUID userToUnfollowId) {
        FollowId followId = new FollowId();
        followId.setFollowerId(currentUserId);
        followId.setFollowingId(userToUnfollowId);

        if (!followRepository.existsById(followId)) {
            throw new IllegalStateException("You are not following this user.");
        }

        followRepository.deleteById(followId);
    }

    // --- CÁC HÀM MỚI QUAN TRỌNG CẦN THÊM ---

    // Đếm số người đang theo dõi user này (Followers)
    public long countFollowers(UUID userId) {
        return followRepository.countByFollowingId(userId);
    }

    // Đếm số người user này đang theo dõi (Following)
    public long countFollowing(UUID userId) {
        return followRepository.countByFollowerId(userId);
    }
    public boolean isFollowing(UUID followerId, UUID followingId) {
        if (followerId == null || followingId == null) return false;
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }
}