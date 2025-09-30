package com.cinetaste.userservice.repository;

import com.cinetaste.userservice.entity.Follow;
import com.cinetaste.userservice.entity.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    // Chúng ta có thể thêm các phương thức truy vấn tùy chỉnh ở đây sau này
    // Ví dụ: countByFollowerId(UUID userId) để đếm số người mình đang theo dõi
}