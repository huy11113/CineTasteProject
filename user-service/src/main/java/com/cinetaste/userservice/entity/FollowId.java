package com.cinetaste.userservice.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
@Embeddable // Đánh dấu đây là một lớp có thể được nhúng
public class FollowId implements Serializable {

    private UUID followerId;
    private UUID followingId;

    // Lombok sẽ tự động tạo constructor, equals(), và hashCode() cần thiết
}