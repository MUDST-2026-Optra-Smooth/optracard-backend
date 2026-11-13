-- Auction marketplace schema. Safe to run on an existing PostgreSQL database.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS auction_id INT;

CREATE TABLE IF NOT EXISTS auctions (
    auction_id SERIAL PRIMARY KEY,
    seller_user_id INT NOT NULL REFERENCES users_admins(ua_id),
    store_id INT REFERENCES marketplace_stores(store_id),
    auction_source VARCHAR(20) NOT NULL CHECK (auction_source IN ('MARKETPLACE', 'OFFICIAL')),
    auction_status VARCHAR(30) NOT NULL,
    auction_title VARCHAR(255) NOT NULL,
    game_id INT REFERENCES cardgames(game_id),
    auction_type VARCHAR(100),
    auction_image_url TEXT,
    auction_description TEXT,
    auction_starting_price DECIMAL(10, 2) NOT NULL,
    bid_increment DECIMAL(10, 2),
    auction_current_price DECIMAL(10, 2) NOT NULL,
    auction_bid_count INT NOT NULL DEFAULT 0,
    auction_starts_at TIMESTAMP NOT NULL,
    auction_ends_at TIMESTAMP NOT NULL,
    auction_checkout_deadline TIMESTAMP,
    auction_winning_user_id INT REFERENCES users_admins(ua_id),
    auction_review_note TEXT,
    auction_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auction_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auction_version BIGINT
);

CREATE TABLE IF NOT EXISTS auction_bids (
    auction_bid_id SERIAL PRIMARY KEY,
    auction_id INT NOT NULL REFERENCES auctions(auction_id) ON DELETE CASCADE,
    bidder_user_id INT NOT NULL REFERENCES users_admins(ua_id),
    bid_amount DECIMAL(10, 2) NOT NULL,
    bidder_public_code VARCHAR(30) NOT NULL,
    bid_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_auctions_status_ends_at ON auctions (auction_status, auction_ends_at);
CREATE INDEX IF NOT EXISTS ix_auctions_seller_created_at ON auctions (seller_user_id, auction_created_at DESC);
CREATE INDEX IF NOT EXISTS ix_auction_bids_auction_created_at ON auction_bids (auction_id, bid_created_at DESC);
