package com.cinetaste.userservice.repository;

import com.cinetaste.userservice.entity.Follow;
import com.cinetaste.userservice.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    long countByFollowingId(UUID userId);
    long countByFollowerId(UUID userId);

    // --- HÀM MỚI: Kiểm tra A có follow B không ---
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
}