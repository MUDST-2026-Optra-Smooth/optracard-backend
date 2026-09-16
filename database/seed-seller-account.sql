-- Local demo seller account for testing the approved-seller flow.
-- Login: seller.demo@optracard.local / SellerDemo!2026
INSERT INTO users_admins (ua_username, ua_password, ua_role, ua_email, ua_phone, ua_address)
VALUES (
    'aaa_trading_seller',
    '$2a$10$V9Pl9HeYUFSbQYSYmrlSYucbAzvAlUL21L3JFR9XxzrMlJe/rlfXe',
    'SELLER',
    'seller.demo@optracard.local',
    '0812345678',
    'Bangkok, Thailand'
)
ON CONFLICT (ua_email) DO UPDATE SET
    ua_username = EXCLUDED.ua_username,
    ua_password = EXCLUDED.ua_password,
    ua_role = EXCLUDED.ua_role,
    ua_phone = EXCLUDED.ua_phone,
    ua_address = EXCLUDED.ua_address;

INSERT INTO marketplace_stores (
    seller_user_id, store_name, store_slug, store_status, store_description,
    physical_store, owner_first_name, owner_last_name, owner_email, owner_phone,
    store_address, province, district, subdistrict, postal_code,
    terms_accepted, submitted_at, reviewed_at, review_note
)
SELECT ua_id, 'AAA-Trading', 'aaa-trading', 'APPROVED', 'Approved local demo seller shop',
       FALSE, 'AAA', 'Trading', 'seller.demo@optracard.local', '0812345678',
       'Bangkok, Thailand', 'Bangkok', 'Pathum Wan', 'Lumphini', '10330',
       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'Demo seller account approved for local testing.'
FROM users_admins
WHERE ua_email = 'seller.demo@optracard.local'
ON CONFLICT (store_slug) DO UPDATE SET
    seller_user_id = EXCLUDED.seller_user_id,
    store_status = 'APPROVED',
    store_name = EXCLUDED.store_name,
    store_description = EXCLUDED.store_description,
    terms_accepted = TRUE,
    reviewed_at = CURRENT_TIMESTAMP,
    review_note = EXCLUDED.review_note;
