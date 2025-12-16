-- ============================================================================
-- Migration: Thêm bảng comment_reactions
-- File: recipe-service/src/main/resources/db/migration/V2__Add_Comment_Reactions.sql
-- ============================================================================

-- Tạo bảng comment_reactions
CREATE TABLE comment_reactions (
    user_id UUID NOT NULL,
    comment_id BIGINT NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    reaction_type VARCHAR(10) NOT NULL CHECK (reaction_type IN ('like', 'dislike')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, comment_id),

    -- Constraint: User chỉ có thể react một lần cho mỗi comment
    CONSTRAINT unique_user_comment_reaction UNIQUE (user_id, comment_id)
);

-- ============================================================================
-- INDEXES (Tối ưu performance)
-- ============================================================================

-- Index cho việc đếm reactions theo comment
CREATE INDEX idx_comment_reactions_comment_id ON comment_reactions(comment_id);

-- Index cho việc query reactions của một user
CREATE INDEX idx_comment_reactions_user_id ON comment_reactions(user_id);

-- Composite index cho việc đếm reactions theo loại
CREATE INDEX idx_comment_reactions_type ON comment_reactions(comment_id, reaction_type);

-- Index cho việc kiểm tra user đã react chưa
CREATE INDEX idx_comment_reactions_user_comment ON comment_reactions(user_id, comment_id);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE comment_reactions IS 'Bảng lưu trữ reactions (like/dislike) của users cho comments';
COMMENT ON COLUMN comment_reactions.reaction_type IS 'Loại reaction: like hoặc dislike';
COMMENT ON COLUMN comment_reactions.created_at IS 'Thời điểm user thực hiện reaction';