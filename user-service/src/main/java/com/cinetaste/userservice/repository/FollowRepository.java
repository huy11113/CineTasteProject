package com.cinetaste.userservice.repository;

import com.cinetaste.userservice.entity.Follow;
import com.cinetaste.userservice.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID; // Thêm import này

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    // --- PHƯƠNG THỨC MỚI: Đếm xem user có bao nhiêu người theo dõi (follower) ---
    long countByFollowingId(UUID userId);

    // --- PHƯƠNG THỨC MỚI: Đếm xem user đang theo dõi bao nhiêu người (following) ---
    long countByFollowerId(UUID userId);
}