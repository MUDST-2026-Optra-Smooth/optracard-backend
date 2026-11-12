-- Persist buyer, seller, and administrator profile avatars as validated image data URLs.
ALTER TABLE users_admins ADD COLUMN IF NOT EXISTS ua_profileimage TEXT;
