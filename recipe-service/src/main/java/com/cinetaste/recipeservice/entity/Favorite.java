package com.cinetaste.recipeservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
// import java.util.UUID; // <-- Không cần import UUID ở đây nữa

@Data
@Entity
@Table(name = "favorites")
public class Favorite {

    @EmbeddedId
    private FavoriteId id; // Chứa cả userId và recipeId

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("recipeId") // Báo cho Hibernate: hãy dùng trường 'recipeId' trong 'id'
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    // --- ĐÃ XÓA TRƯỜNG BỊ TRÙNG LẶP ---
    // @Column(name = "user_id", insertable = false, updatable = false)
    // private UUID userId;
    // --- KẾT THÚC XÓA ---
    // (Hibernate sẽ tự động hiểu trường 'userId' trong 'id' chính là cột 'user_id' còn lại)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}