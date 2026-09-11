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

ALTER TABLE products ADD COLUMN IF NOT EXISTS pro_sku VARCHAR(80);
ALTER TABLE products ADD COLUMN IF NOT EXISTS pro_listing_source VARCHAR(20) DEFAULT 'OFFICIAL';
ALTER TABLE products ADD COLUMN IF NOT EXISTS store_id INT;

UPDATE products
SET pro_listing_source = 'OFFICIAL'
WHERE pro_listing_source IS NULL;

ALTER TABLE products
    ALTER COLUMN pro_listing_source SET DEFAULT 'OFFICIAL',
    ALTER COLUMN pro_listing_source SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_products_pro_sku
    ON products (pro_sku)
    WHERE pro_sku IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_products_listing_source
    ON products (pro_listing_source);

CREATE INDEX IF NOT EXISTS ix_products_store_id
    ON products (store_id);

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
