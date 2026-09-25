-- Development/admin seed accounts.
-- Passwords are BCrypt hashes; do not replace them with plaintext values.
-- Re-running this script updates the same accounts instead of creating duplicates.

INSERT INTO users_admins (ua_username, ua_password, ua_role, ua_email)
VALUES
    ('admin_master', '$2a$10$q9WFfMGK7MX4xvWv1XWM0u8dHZ6.Pd/.uFKCbOCoF6QSxYXM1c1G2', 'ADMIN', 'admin@optracard.com'),
    ('superadmin_master', '$2a$10$JKCxxZB5hasBs9z4kDjX.OyfRDxMED/AVsOdsNLhXy9VS78KlMlyG', 'SUPER_ADMIN', 'superadmin@optracard.com')
ON CONFLICT (ua_email) DO UPDATE SET
    ua_username = EXCLUDED.ua_username,
    ua_password = EXCLUDED.ua_password,
    ua_role = EXCLUDED.ua_role;
