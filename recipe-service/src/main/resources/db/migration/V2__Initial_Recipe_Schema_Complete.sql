-- Kích hoạt extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Bảng movies
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
    author_id UUID, -- ✅ Không NOT NULL, cho phép SET NULL
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
    avg_rating NUMERIC(3, 2) NOT NULL DEFAULT 0.00 CHECK (avg_rating >= 0 AND avg_rating <= 5),
    ratings_count INT NOT NULL DEFAULT 0 CHECK (ratings_count >= 0),
    comments_count INT NOT NULL DEFAULT 0 CHECK (comments_count >= 0),
    favorites_count INT NOT NULL DEFAULT 0 CHECK (favorites_count >= 0),
    views_count INT NOT NULL DEFAULT 0 CHECK (views_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT check_status CHECK (status IN ('draft', 'published', 'archived')),
    CONSTRAINT check_visibility CHECK (visibility IN ('public', 'private', 'unlisted'))
);

-- 3. Bảng recipe_steps
CREATE TABLE recipe_steps (
    id BIGSERIAL PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    step_order SMALLINT NOT NULL CHECK (step_order > 0),
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
    author_id UUID,
    parent_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT check_no_self_parent CHECK (id != parent_id)
);

-- 7. Bảng tags
CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(30),
    slug VARCHAR(50) UNIQUE NOT NULL
);

-- 8. Bảng recipe_tags
CREATE TABLE recipe_tags (
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    tag_id INT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, tag_id)
);

-- 9. Bảng recipe_ratings
CREATE TABLE recipe_ratings (
    user_id UUID NOT NULL,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, recipe_id)
);

-- 10. Bảng favorites
CREATE TABLE favorites (
    user_id UUID NOT NULL,
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, recipe_id)
);

-- 11. Bảng ai_feedback
CREATE TABLE ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    recipe_id UUID REFERENCES recipes(id) ON DELETE SET NULL,
    ai_feature VARCHAR(50) NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 12. Bảng ai_requests_log
CREATE TABLE ai_requests_log (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID,
    feature VARCHAR(50) NOT NULL,
    request_payload_summary TEXT,
    response_payload_summary TEXT,
    duration_ms INT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 13. Bảng notifications (MỚI)
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    related_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ
);

-- === INDEXES ===
CREATE INDEX idx_recipes_author_id ON recipes(author_id);
CREATE INDEX idx_recipes_status ON recipes(status);
CREATE INDEX idx_recipes_created_at ON recipes(created_at DESC);
CREATE INDEX idx_recipes_title_fts ON recipes USING gin(to_tsvector('english', title));

CREATE INDEX idx_comments_recipe_id ON comments(recipe_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);

CREATE INDEX idx_recipe_ratings_recipe_id ON recipe_ratings(recipe_id);
CREATE INDEX idx_favorites_user_id ON favorites(user_id);

CREATE INDEX idx_ai_feedback_user_id ON ai_feedback(user_id);
CREATE INDEX idx_ai_requests_log_feature ON ai_requests_log(feature);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);

-- === TRIGGERS ===
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_recipes_updated_at
    BEFORE UPDATE ON recipes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Auto-generate slug
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

-- Update rating stats
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
        ratings_count = (SELECT COUNT(*) FROM recipe_ratings WHERE recipe_id = target_recipe_id),
        avg_rating = (SELECT COALESCE(ROUND(AVG(rating)::numeric, 2), 0) FROM recipe_ratings WHERE recipe_id = target_recipe_id)
    WHERE id = target_recipe_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_recipe_rating_stats
    AFTER INSERT OR UPDATE OR DELETE ON recipe_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_recipe_rating_stats();

-- Update comments count
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
    SET comments_count = (SELECT COUNT(*) FROM comments WHERE recipe_id = target_recipe_id AND deleted_at IS NULL)
    WHERE id = target_recipe_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_recipe_comments_count
    AFTER INSERT OR UPDATE OR DELETE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_recipe_comments_count();

-- Update favorites count
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
    SET favorites_count = (SELECT COUNT(*) FROM favorites WHERE recipe_id = target_recipe_id)
    WHERE id = target_recipe_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_recipe_favorites_count
    AFTER INSERT OR DELETE ON favorites
    FOR EACH ROW
    EXECUTE FUNCTION update_recipe_favorites_count();