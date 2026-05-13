-- V7 の refresh_token_hash (VARCHAR 255, BCrypt用) を SHA-256用に付け替え
-- SHA-256 の hex は 64 文字固定なので長さ変更 + prefix カラム追加

ALTER TABLE users
  ADD COLUMN refresh_token_prefix VARCHAR(8),
  ALTER COLUMN refresh_token_hash TYPE VARCHAR(64);

-- 既存の BCrypt ハッシュ値は新方式と非互換のため全クリア
UPDATE users SET refresh_token_prefix = NULL, refresh_token_hash = NULL, refresh_token_expires_at = NULL;

CREATE INDEX idx_users_refresh_token_prefix ON users (refresh_token_prefix);
