-- Run this once in DBeaver for an existing Optracard database.
-- A fresh database already receives the same structure from database/init.sql.

CREATE TABLE IF NOT EXISTS marketplace_stores (
    store_id SERIAL PRIMARY KEY,
    seller_user_id INT NOT NULL REFERENCES users_admins(ua_id),
    store_name VARCHAR(255) NOT NULL,
    store_slug VARCHAR(100) NOT NULL UNIQUE,
    store_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    store_description TEXT
);

-- Seller application details collected by the My Shop wizard. These columns
-- keep an application in PENDING status until an administrator reviews it.
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS physical_store BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS owner_first_name VARCHAR(255);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS owner_last_name VARCHAR(255);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS owner_email VARCHAR(255);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS owner_phone VARCHAR(50);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS bank_branch VARCHAR(255);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS bank_account_name VARCHAR(255);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS bank_account_number VARCHAR(100);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS store_address TEXT;
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS province VARCHAR(100);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS subdistrict VARCHAR(100);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20);
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS store_profile_image TEXT;
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS bank_passbook_image TEXT;
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS terms_accepted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE marketplace_stores ADD COLUMN IF NOT EXISTS review_note TEXT;
UPDATE marketplace_stores SET physical_store = FALSE WHERE physical_store IS NULL;
ALTER TABLE marketplace_stores ALTER COLUMN physical_store SET DEFAULT FALSE, ALTER COLUMN physical_store SET NOT NULL;
UPDATE marketplace_stores SET terms_accepted = FALSE WHERE terms_accepted IS NULL;
ALTER TABLE marketplace_stores ALTER COLUMN terms_accepted SET DEFAULT FALSE, ALTER COLUMN terms_accepted SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_marketplace_stores_seller_user'
    ) THEN
        ALTER TABLE marketplace_stores
            ADD CONSTRAINT fk_marketplace_stores_seller_user
            FOREIGN KEY (seller_user_id) REFERENCES users_admins(ua_id);
    END IF;
END $$;

-- Checkout metadata. Card numbers are intentionally not stored; a real
-- payment provider should tokenize them and only return a payment reference.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_recipient_name VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_recipient_phone VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_shipping_method VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_shipping_fee DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
-- Store a seller snapshot on every order. This lets an Official item and a
-- Marketplace item remain separate throughout checkout and order history.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_store_id INT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_store_name VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ord_source VARCHAR(20) NOT NULL DEFAULT 'OFFICIAL';
UPDATE orders SET ord_payment_status = 'PENDING' WHERE ord_payment_status IS NULL;
UPDATE orders SET ord_source = 'OFFICIAL' WHERE ord_source IS NULL;
ALTER TABLE orders ALTER COLUMN ord_payment_status SET DEFAULT 'PENDING', ALTER COLUMN ord_payment_status SET NOT NULL;
ALTER TABLE orders ALTER COLUMN ord_source SET DEFAULT 'OFFICIAL', ALTER COLUMN ord_source SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_marketplace_stores_seller_user
    ON marketplace_stores (seller_user_id, store_id DESC);
CREATE INDEX IF NOT EXISTS ix_orders_user_created
    ON orders (ua_id, ord_createdate DESC);
CREATE INDEX IF NOT EXISTS ix_orders_store_created
    ON orders (ord_store_id, ord_createdate DESC);

ALTER TABLE products ADD COLUMN IF NOT EXISTS pro_listing_source VARCHAR(20) DEFAULT 'OFFICIAL';
ALTER TABLE products ADD COLUMN IF NOT EXISTS store_id INT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS pro_set VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS pro_language VARCHAR(100);
ALTER TABLE products ADD COLUMN IF NOT EXISTS pro_approval_status VARCHAR(20) DEFAULT 'APPROVED';
ALTER TABLE products ADD COLUMN IF NOT EXISTS pro_template_id INT;
UPDATE products SET pro_approval_status = 'APPROVED' WHERE pro_approval_status IS NULL;
ALTER TABLE products ALTER COLUMN pro_approval_status SET DEFAULT 'APPROVED', ALTER COLUMN pro_approval_status SET NOT NULL;

-- Product details used by the public product-detail page. The old local
-- catalog stored condition/seeded JSON and an internal SKU; neither is part
-- of the product model anymore.
DROP INDEX IF EXISTS ux_products_pro_sku;
ALTER TABLE products DROP COLUMN IF EXISTS pro_sku;
ALTER TABLE products DROP COLUMN IF EXISTS pro_attributes;

UPDATE products
SET pro_listing_source = 'OFFICIAL'
WHERE pro_listing_source IS NULL;

ALTER TABLE products
    ALTER COLUMN pro_listing_source SET DEFAULT 'OFFICIAL',
    ALTER COLUMN pro_listing_source SET NOT NULL;

CREATE INDEX IF NOT EXISTS ix_products_listing_source
    ON products (pro_listing_source);

CREATE INDEX IF NOT EXISTS ix_products_store_id
    ON products (store_id);

CREATE INDEX IF NOT EXISTS ix_products_template_id
    ON products (pro_template_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_template_product'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_template_product
            FOREIGN KEY (pro_template_id) REFERENCES products(pro_id);
    END IF;
END $$;

-- Official catalog metadata.
UPDATE products SET pro_set = 'Surging Sparks [SV8]', pro_language = 'English'
WHERE pro_name = 'Pikachu ex Special Illustration';
UPDATE products SET pro_set = 'Romance Dawn [OP01]', pro_language = 'English'
WHERE pro_name = 'Monkey D. Luffy Leader Parallel';
UPDATE products SET pro_set = 'Quarter Century Chronicle', pro_language = 'English'
WHERE pro_name = 'Dark Magician 25th Anniversary';
UPDATE products SET pro_set = 'Dominaria United [DMU]', pro_language = 'English'
WHERE pro_name = 'Liliana of the Veil Borderless';
UPDATE products SET pro_set = 'Scarlet & Violet—Journey Together [SV9]', pro_language = 'English'
WHERE pro_name IN ('Pokémon Journey Together Booster Pack', 'Pokémon Journey Together Booster Box');
UPDATE products SET pro_set = 'Royal Blood [OP-10]', pro_language = 'Japanese'
WHERE pro_name IN ('One Piece OP-10 Royal Blood Booster', 'One Piece OP-10 Booster Box');
UPDATE products SET pro_set = 'Alliance Insight [ALIN]', pro_language = 'English'
WHERE pro_name IN ('Yu-Gi-Oh! Alliance Insight Booster', 'Yu-Gi-Oh! Alliance Insight Box');
UPDATE products SET pro_set = 'Aetherdrift [DFT]', pro_language = 'English'
WHERE pro_name IN ('MTG Aetherdrift Play Booster', 'MTG Aetherdrift Play Booster Box');
UPDATE products SET pro_set = 'Fusion Strike [SWSH8]', pro_language = 'English'
WHERE pro_name = 'Gengar VMAX Alternate Art';
UPDATE products SET pro_set = 'Romance Dawn [OP01]', pro_language = 'English'
WHERE pro_name = 'Nami Manga Rare';
UPDATE products SET pro_set = 'Ghosts From the Past: The 2nd Haunting [GFP2]', pro_language = 'English'
WHERE pro_name = 'Blue-Eyes White Dragon Ghost Rare';
UPDATE products SET pro_set = 'Double Masters 2022 [2X2]', pro_language = 'English'
WHERE pro_name = 'Mana Crypt Borderless';
UPDATE products SET pro_set = 'Pokémon Card 151', pro_language = 'Korean'
WHERE pro_name = 'Pokémon 151 Korean Booster Pack';
UPDATE products SET pro_set = 'Premium Booster -The Best- [PRB-01]', pro_language = 'Japanese'
WHERE pro_name IN ('One Piece PRB-01 The Best Booster', 'One Piece PRB-01 Booster Box');
UPDATE products SET pro_set = 'Quarter Century Bonanza', pro_language = 'English'
WHERE pro_name = 'Yu-Gi-Oh! Quarter Century Bonanza Pack';
UPDATE products SET pro_set = 'Magic: The Gathering Foundations [FDN]', pro_language = 'English'
WHERE pro_name = 'MTG Foundations Collector Booster';
UPDATE products SET pro_set = 'Pokémon Card 151', pro_language = 'Japanese'
WHERE pro_name = 'Pokémon 151 Japanese Booster Box';
UPDATE products SET pro_set = '25th Anniversary Rarity Collection', pro_language = 'English'
WHERE pro_name = 'Yu-Gi-Oh! Rarity Collection Box';
UPDATE products SET pro_set = 'Modern Horizons 3 [MH3]', pro_language = 'English'
WHERE pro_name = 'MTG Modern Horizons 3 Play Box';
UPDATE products SET pro_set = 'OP-16', pro_language = 'Japanese'
WHERE pro_name = 'Monkey D. Luffy OP-16';
UPDATE products SET pro_set = 'Unified Minds [SM11]', pro_language = 'English'
WHERE pro_name = 'Raichu & Alolan Raichu GX';
UPDATE products SET pro_set = 'BT07 Life of หน่วง', pro_language = 'Thai'
WHERE pro_name = 'พี่หน่วง พิธีกรผมสวย';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_products_listing_source'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT ck_products_listing_source
            CHECK (pro_listing_source IN ('OFFICIAL', 'MARKETPLACE'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_products_source_store'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT ck_products_source_store
            CHECK (
                (pro_listing_source = 'OFFICIAL' AND store_id IS NULL)
                OR (pro_listing_source = 'MARKETPLACE' AND store_id IS NOT NULL)
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_products_approval_status'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT ck_products_approval_status
            CHECK (pro_approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_marketplace_store'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_marketplace_store
            FOREIGN KEY (store_id) REFERENCES marketplace_stores(store_id) ON DELETE SET NULL;
    END IF;
END $$;
