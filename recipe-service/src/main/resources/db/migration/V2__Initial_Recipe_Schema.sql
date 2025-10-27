-- Kích hoạt extension cần thiết cho việc tạo UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========= BẢNG CỦA RECIPE SERVICE =========

-- 1. Bảng movies (Cache thông tin)
CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tmdb_id INT UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    overview TEXT,
    poster_url TEXT,
    release_date DATE,
    genres JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Bảng recipes
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL, -- ĐÃ XÓA "REFERENCES users(id)"
    movie_id UUID REFERENCES movies(id),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    summary TEXT,
    difficulty SMALLINT CHECK (difficulty >= 1 AND difficulty <= 5),
    prep_time_minutes INT,
    cook_time_minutes INT,
    servings SMALLINT,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    visibility VARCHAR(20) NOT NULL DEFAULT 'public',
    main_image_url TEXT,
    nutrition_info JSONB,
    avg_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00,
    ratings_count INT NOT NULL DEFAULT 0,
    comments_count INT NOT NULL DEFAULT 0,
    favorites_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

-- 3. Bảng recipe_steps
CREATE TABLE recipe_steps (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_order SMALLINT NOT NULL,
    instructions TEXT NOT NULL,
    image_url TEXT,
    UNIQUE (recipe_id, step_order)
);

-- 4. Bảng ingredients
CREATE TABLE ingredients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(50)
);

-- 5. Bảng recipe_ingredients
CREATE TABLE recipe_ingredients (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id),
    quantity NUMERIC(10, 2),
    unit VARCHAR(50),
    notes TEXT
);

-- 6. Bảng comments
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    author_id UUID NOT NULL, -- ĐÃ XÓA "REFERENCES users(id)"
    parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

-- 7. Bảng tags và recipe_tags
CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(30)
);

CREATE TABLE recipe_tags (
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    tag_id INT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, tag_id)
);

-- 8. Bảng recipe_ratings
CREATE TABLE recipe_ratings (
    user_id UUID NOT NULL, -- ĐÃ XÓA "REFERENCES users(id)"
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, recipe_id)
);

-- 9. Bảng favorites (Bookmarks)
CREATE TABLE favorites (
    user_id UUID NOT NULL, -- ĐÃ XÓA "REFERENCES users(id)"
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, recipe_id)
);

-- 10. Bảng ai_feedback
CREATE TABLE ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL, -- ĐÃ XÓA "REFERENCES users(id)"
    recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL,
    ai_feature VARCHAR(50) NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 11. Bảng ai_requests_log
CREATE TABLE ai_requests_log (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID, -- ĐÃ XÓA "REFERENCES users(id)"
    feature VARCHAR(50) NOT NULL,
    request_payload_summary TEXT,
    response_payload_summary TEXT,
    duration_ms INT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- === CÁC CHỈ MỤC (INDEX) ===
CREATE INDEX idx_recipes_author_id ON recipes(author_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);
CREATE INDEX idx_ai_feedback_user_id ON ai_feedback(user_id);
CREATE INDEX idx_ai_requests_log_feature ON ai_requests_log(feature);