-- =============================================================
-- V3 : identity and access  (role, users, user_role, refresh_token)
-- The table is called "users" because "user" is a reserved word in PostgreSQL.
-- =============================================================

CREATE TABLE role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(200),
    CONSTRAINT chk_role_name CHECK (name IN
        ('ADMIN','AUTHORITY_OFFICIAL','FIELD_OFFICER','LOGISTICS_MANAGER','DRIVER'))
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,   -- BCrypt output, never a plain password
    full_name     VARCHAR(120) NOT NULL,
    phone         VARCHAR(15),
    district_id   BIGINT REFERENCES district(id),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50)
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES role(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_token (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,  -- only the hash, never the token itself
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE
);
