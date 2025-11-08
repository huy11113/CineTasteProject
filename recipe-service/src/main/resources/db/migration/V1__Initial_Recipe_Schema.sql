

-- ============================================
-- PHẦN 2: BẢNG NỘI DUNG CHÍNH (do recipe-service quản lý)
-- ============================================

-- 5. Bảng movies (Cache TMDB data)
CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tmdb_id INT UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    overview TEXT,
    poster_url TEXT,
    backdrop_url TEXT, -- MỚI
    release_date DATE,
    runtime INT, -- MỚI - thời lượng phim (phút)
    vote_average NUMERIC(3,1), -- MỚI - điểm TMDB
    vote_count INT, -- MỚI
    genres JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Constraints
    CONSTRAINT check_tmdb_id CHECK (tmdb_id > 0),
    CONSTRAINT check_runtime CHECK (runtime IS NULL OR runtime > 0)
);

-- 6. Bảng recipes
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Cần bảng 'users' từ V1 của user-service, nhưng không tạo khóa ngoại
    -- Vì đây là 2 service riêng biệt. Logic sẽ được xử lý ở tầng ứng dụng.
    author_id UUID,

    movie_id UUID REFERENCES movies(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    summary TEXT,
    difficulty SMALLINT CHECK (difficulty >= 1 AND difficulty <= 5),
    prep_time_minutes INT CHECK (prep_time_minutes >= 0),
    cook_time_minutes INT CHECK (cook_time_minutes >= 0),
    servings SMALLINT CHECK (servings > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    visibility VARCHAR(20) NOT NULL DEFAULT 'public',
    main_image_url TEXT,
    nutrition_info JSONB,

    -- Cached counters
    avg_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00 CHECK (avg_rating >= 0 AND avg_rating <= 5),
    ratings_count INT NOT NULL DEFAULT 0 CHECK (ratings_count >= 0),
    comments_count INT NOT NULL DEFAULT 0 CHECK (comments_count >= 0),
    favorites_count INT NOT NULL DEFAULT 0 CHECK (favorites_count >= 0),
    views_count INT NOT NULL DEFAULT 0 CHECK (views_count >= 0), -- MỚI

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ, -- MỚI
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT check_status CHECK (status IN ('draft', 'published', 'archived')),
    CONSTRAINT check_visibility CHECK (visibility IN ('public', 'private', 'unlisted')),
    CONSTRAINT check_slug_format CHECK (slug ~ '^[a-z0-9]+(?:-[a-z0-9]+)*$')
);

-- 7. Bảng recipe_steps
CREATE TABLE recipe_steps (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_order SMALLINT NOT NULL CHECK (step_order > 0),
    instructions TEXT NOT NULL,
    image_url TEXT,
    duration_minutes INT CHECK (duration_minutes >= 0), -- MỚI

    UNIQUE (recipe_id, step_order)
);

-- 8. Bảng ingredients (Chuẩn hóa)
CREATE TABLE ingredients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(50),
    is_allergen BOOLEAN DEFAULT FALSE, -- MỚI
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Constraint
    CONSTRAINT check_name_not_empty CHECK (trim(name) != '')
);

-- 9. Bảng recipe_ingredients
CREATE TABLE recipe_ingredients (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id) ON DELETE RESTRICT,
    quantity NUMERIC(10, 2) CHECK (quantity > 0),
    unit VARCHAR(50),
    notes TEXT,
    is_optional BOOLEAN DEFAULT FALSE, -- MỚI

    UNIQUE (recipe_id, ingredient_id)
);

-- ============================================
-- PHẦN 3: TƯƠNG TÁC CỘNG ĐỒNG (do recipe-service quản lý)
-- ============================================

-- 10. Bảng comments
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,

    -- ID của user từ user-service
    author_id UUID,

    parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT check_no_self_parent CHECK (id != parent_id),
    CONSTRAINT check_content_not_empty CHECK (trim(content) != '')
);

-- 11. Bảng tags
CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(30),
    slug VARCHAR(50) UNIQUE NOT NULL, -- MỚI
    description TEXT, -- MỚI
    usage_count INT NOT NULL DEFAULT 0, -- MỚI
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Constraint
    CONSTRAINT check_tag_type CHECK (type IN ('cuisine', 'dietary', 'occasion', 'difficulty', 'movie', 'theme'))
);

-- 12. Bảng recipe_tags (Many-to-Many)
CREATE TABLE recipe_tags (
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    tag_id INT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (recipe_id, tag_id)
);

-- 13. Bảng recipe_ratings
CREATE TABLE recipe_ratings (
    user_id UUID NOT NULL, -- ID của user từ user-service
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,

    PRIMARY KEY (user_id, recipe_id)
);

-- 14. Bảng favorites (Bookmarks)
CREATE TABLE favorites (
    user_id UUID NOT NULL, -- ID của user từ user-service
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, recipe_id)
);

-- 15. Bảng recipe_views (MỚI - tracking views)
CREATE TABLE recipe_views (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id UUID, -- ID của user từ user-service
    ip_address INET,
    user_agent TEXT,
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================
-- PHẦN 4: AI & LEARNING LOOP (do recipe-service quản lý)
-- ============================================

-- 16. Bảng ai_feedback
CREATE TABLE ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL,
    ai_feature VARCHAR(50) NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Constraint
    CONSTRAINT check_ai_feature CHECK (ai_feature IN ('analyze-dish', 'modify-recipe', 'create-by-theme', 'critique-dish'))
);

-- 17. Bảng ai_requests_log - ĐƠN GIẢN HÓA (KHÔNG DÙNG PARTITION)
CREATE TABLE ai_requests_log (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID,
    feature VARCHAR(50) NOT NULL,
    request_payload_summary TEXT,
    response_payload_summary TEXT,
    duration_ms INT CHECK (duration_ms >= 0),
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Tạo partitions (ví dụ cho 6 tháng)
CREATE TABLE ai_requests_log_2025_p1 PARTITION OF ai_requests_log
    FOR VALUES FROM ('2025-01-01') TO ('2025-07-01');
CREATE TABLE ai_requests_log_2025_p2 PARTITION OF ai_requests_log
    FOR VALUES FROM ('2025-07-01') TO ('2026-01-01');
-- (Bạn có thể thêm nhiều partition chi tiết hơn sau)

-- ============================================
-- PHẦN 5: BẢNG BỔ SUNG (do recipe-service quản lý)
-- ============================================

-- 18. Bảng notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL, -- ID của user từ user-service
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    related_id UUID, -- ID của recipe/comment/user liên quan
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ,

    CONSTRAINT check_notification_type CHECK (type IN ('new_follower', 'new_comment', 'new_rating', 'recipe_featured', 'system'))
);

-- 19. Bảng reports (Báo cáo vi phạm)
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id UUID NOT NULL, -- ID của user từ user-service
    reported_user_id UUID, -- ID của user từ user-service
    reported_recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,
    reported_comment_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    resolved_by UUID, -- ID của admin/mod từ user-service
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT check_report_target CHECK (
        (reported_user_id IS NOT NULL) OR
        (reported_recipe_id IS NOT NULL) OR
        (reported_comment_id IS NOT NULL)
    ),
    CONSTRAINT check_report_reason CHECK (reason IN ('spam', 'inappropriate', 'copyright', 'harassment', 'other')),
    CONSTRAINT check_report_status CHECK (status IN ('pending', 'reviewing', 'resolved', 'dismissed'))
);

-- 20. Bảng audit_logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID, -- ID của user từ user-service
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id TEXT NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================
-- PHẦN 6: INDEXES (cho recipe-service)
-- ============================================

-- === MOVIES INDEXES ===
CREATE INDEX idx_movies_tmdb_id ON movies(tmdb_id);
CREATE INDEX idx_movies_release_date ON movies(release_date);
CREATE INDEX idx_movies_vote_average ON movies(vote_average DESC);

-- === RECIPES INDEXES ===
CREATE INDEX idx_recipes_author_id ON recipes(author_id);
CREATE INDEX idx_recipes_movie_id ON recipes(movie_id);
CREATE INDEX idx_recipes_status ON recipes(status);
CREATE INDEX idx_recipes_visibility ON recipes(visibility);
CREATE INDEX idx_recipes_created_at ON recipes(created_at DESC);
CREATE INDEX idx_recipes_published_at ON recipes(published_at DESC) WHERE published_at IS NOT NULL;
CREATE INDEX idx_recipes_deleted_at ON recipes(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_recipes_avg_rating ON recipes(avg_rating DESC);
CREATE INDEX idx_recipes_title_fts ON recipes USING gin(to_tsvector('english', title));
CREATE INDEX idx_recipes_summary_fts ON recipes USING gin(to_tsvector('english', COALESCE(summary, '')));
CREATE INDEX idx_recipes_status_visibility ON recipes(status, visibility);
CREATE INDEX idx_recipes_author_status ON recipes(author_id, status);

-- === COMMENTS INDEXES ===
CREATE INDEX idx_comments_recipe_id ON comments(recipe_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);
CREATE INDEX idx_comments_created_at ON comments(created_at DESC);
CREATE INDEX idx_comments_deleted_at ON comments(deleted_at) WHERE deleted_at IS NOT NULL;

-- === RECIPE_STEPS INDEXES ===
CREATE INDEX idx_recipe_steps_recipe_id ON recipe_steps(recipe_id);
CREATE INDEX idx_recipe_steps_order ON recipe_steps(recipe_id, step_order);

-- === INGREDIENTS INDEXES ===
CREATE INDEX idx_ingredients_name ON ingredients(name);
CREATE INDEX idx_ingredients_category ON ingredients(category);
CREATE INDEX idx_ingredients_name_fts ON ingredients USING gin(to_tsvector('english', name));

-- === RECIPE_INGREDIENTS INDEXES ===
CREATE INDEX idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id);
CREATE INDEX idx_recipe_ingredients_ingredient_id ON recipe_ingredients(ingredient_id);

-- === TAGS INDEXES ===
CREATE INDEX idx_tags_type ON tags(type);
CREATE INDEX idx_tags_slug ON tags(slug);
CREATE INDEX idx_tags_usage_count ON tags(usage_count DESC);

-- === RECIPE_TAGS INDEXES ===
CREATE INDEX idx_recipe_tags_recipe_id ON recipe_tags(recipe_id);
CREATE INDEX idx_recipe_tags_tag_id ON recipe_tags(tag_id);

-- === RECIPE_RATINGS INDEXES ===
CREATE INDEX idx_recipe_ratings_recipe_id ON recipe_ratings(recipe_id);
CREATE INDEX idx_recipe_ratings_user_id ON recipe_ratings(user_id);
CREATE INDEX idx_recipe_ratings_created_at ON recipe_ratings(created_at DESC);

-- === FAVORITES INDEXES ===
CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_recipe_id ON favorites(recipe_id);
CREATE INDEX idx_favorites_created_at ON favorites(created_at DESC);

-- === RECIPE_VIEWS INDEXES ===
CREATE INDEX idx_recipe_views_recipe_id ON recipe_views(recipe_id);
CREATE INDEX idx_recipe_views_user_id ON recipe_views(user_id);
CREATE INDEX idx_recipe_views_viewed_at ON recipe_views(viewed_at DESC);
CREATE INDEX idx_recipe_views_ip_address ON recipe_views(ip_address);

-- === AI FEEDBACK INDEXES ===
CREATE INDEX idx_ai_feedback_user_id ON ai_feedback(user_id);
CREATE INDEX idx_ai_feedback_recipe_id ON ai_feedback(recipe_id);
CREATE INDEX idx_ai_feedback_feature ON ai_feedback(ai_feature);
CREATE INDEX idx_ai_feedback_created_at ON ai_feedback(created_at DESC);

-- === AI REQUESTS LOG INDEXES ===
CREATE INDEX idx_ai_requests_log_user_id ON ai_requests_log(user_id);
CREATE INDEX idx_ai_requests_log_feature ON ai_requests_log(feature);
CREATE INDEX idx_ai_requests_log_created_at ON ai_requests_log(created_at DESC);
CREATE INDEX idx_ai_requests_log_success ON ai_requests_log(success);

-- === NOTIFICATIONS INDEXES ===
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- === REPORTS INDEXES ===
CREATE INDEX idx_reports_reporter_id ON reports(reporter_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);

-- === AUDIT LOGS INDEXES ===
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- ============================================
-- PHẦN 7: FUNCTIONS & TRIGGERS (cho recipe-service)
-- ============================================

-- Function: Tự động cập nhật updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Áp dụng trigger cho các bảng của recipe-service
CREATE TRIGGER trigger_movies_updated_at
    BEFORE UPDATE ON movies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_recipes_updated_at
    BEFORE UPDATE ON recipes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_recipe_ratings_updated_at
    BEFORE UPDATE ON recipe_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function: Auto-generate slug cho recipes
CREATE OR REPLACE FUNCTION generate_recipe_slug()
RETURNS TRIGGER AS $$
DECLARE
    base_slug TEXT;
    final_slug TEXT;
    counter INT := 0;
BEGIN
    IF NEW.slug IS NULL OR NEW.slug = '' OR
       (TG_OP = 'UPDATE' AND OLD.title != NEW.title) THEN

        base_slug := lower(trim(regexp_replace(NEW.title, '[^a-zA-Z0-9]+', '-', 'g'), '-'));
        final_slug := base_slug;

        WHILE EXISTS (SELECT 1 FROM recipes WHERE slug = final_slug AND id != NEW.id) LOOP
            counter := counter + 1;
            final_slug := base_slug || '-' || counter;
        END LOOP;

        NEW.slug := final_slug;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_recipe_slug
    BEFORE INSERT OR UPDATE OF title ON recipes
    FOR EACH ROW
    EXECUTE FUNCTION generate_recipe_slug();

-- Function: Cập nhật recipe rating stats
CREATE OR REPLACE FUNCTION update_recipe_rating_stats()
RETURNS TRIGGER AS $$
DECLARE
    target_recipe_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_recipe_id := OLD.recipe_id;
    ELSE
        target_recipe_id := NEW.recipe_id;
    END IF;

    UPDATE recipes
    SET
        ratings_count = (
            SELECT COUNT(*)
            FROM recipe_ratings
            WHERE recipe_id = target_recipe_id
        ),
        avg_rating = (
            SELECT COALESCE(ROUND(AVG(rating)::numeric, 2), 0)
            FROM recipe_ratings
            WHERE recipe_id = target_recipe_id
        )
    WHERE id = target_recipe_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_recipe_rating_stats
    AFTER INSERT OR UPDATE OR DELETE ON recipe_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_recipe_rating_stats();

-- Function: Cập nhật recipe comments count
CREATE OR REPLACE FUNCTION update_recipe_comments_count()
RETURNS TRIGGER AS $$
DECLARE
    target_recipe_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_recipe_id := OLD.recipe_id;
    ELSE
        target_recipe_id := NEW.recipe_id;
    END IF;

    UPDATE recipes
    SET comments_count = (
        SELECT COUNT(*)
        FROM comments
        WHERE recipe_id = target_recipe_id
          AND deleted_at IS NULL
    )
    WHERE id = target_recipe_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_recipe_comments_count
    AFTER INSERT OR UPDATE OR DELETE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_recipe_comments_count();

-- Function: Cập nhật recipe favorites count
CREATE OR REPLACE FUNCTION update_recipe_favorites_count()
RETURNS TRIGGER AS $$
DECLARE
    target_recipe_id UUID;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_recipe_id := OLD.recipe_id;
    ELSE
        target_recipe_id := NEW.recipe_id;
    END IF;

    UPDATE recipes
    SET favorites_count = (
        SELECT COUNT(*)
        FROM favorites
        WHERE recipe_id = target_recipe_id
    )
    WHERE id = target_recipe_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_recipe_favorites_count
    AFTER INSERT OR DELETE ON favorites
    FOR EACH ROW
    EXECUTE FUNCTION update_recipe_favorites_count();

-- Function: Cập nhật tag usage count
CREATE OR REPLACE FUNCTION update_tag_usage_count()
RETURNS TRIGGER AS $$
DECLARE
    target_tag_id INT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        target_tag_id := OLD.tag_id;
    ELSE
        target_tag_id := NEW.tag_id;
    END IF;

    UPDATE tags
    SET usage_count = (
        SELECT COUNT(*)
        FROM recipe_tags
        WHERE tag_id = target_tag_id
    )
    WHERE id = target_tag_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_tag_usage_count
    AFTER INSERT OR DELETE ON recipe_tags
    FOR EACH ROW
    EXECUTE FUNCTION update_tag_usage_count();

-- Function: Auto-generate tag slug
CREATE OR REPLACE FUNCTION generate_tag_slug()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.slug IS NULL OR NEW.slug = '' THEN
        NEW.slug := lower(trim(regexp_replace(NEW.name, '[^a-zA-Z0-9]+', '-', 'g'), '-'));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generate_tag_slug
    BEFORE INSERT OR UPDATE OF name ON tags
    FOR EACH ROW
    EXECUTE FUNCTION generate_tag_slug();

-- Function: Tự động set published_at khi status = published
CREATE OR REPLACE FUNCTION set_published_at()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'published' AND OLD.status != 'published' THEN
        NEW.published_at := now();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_set_published_at
    BEFORE UPDATE OF status ON recipes
    FOR EACH ROW
    EXECUTE FUNCTION set_published_at();