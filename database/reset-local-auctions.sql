-- ONE-TIME local development reset for auction data.
--
-- Run auction-migration.sql and auction-completion-migration.sql first.
-- This intentionally preserves users, sellers, stores, card games and products.
-- It removes only auction rows and orders produced by auction checkout.

BEGIN;

-- Return the one item reserved for each linked auction before removing it.
UPDATE products AS product
SET pro_quantity = COALESCE(product.pro_quantity, 0) + 1,
    is_active = COALESCE(auction.product_was_active, TRUE),
    auction_reserved = FALSE
FROM auctions AS auction
WHERE auction.product_id = product.pro_id
  AND COALESCE(auction.stock_reserved, FALSE) = TRUE;

-- Auction checkout owns both the order and its immutable item snapshots.
DELETE FROM orderitems AS item
USING orders AS placed_order
WHERE item.ord_id = placed_order.ord_id
  AND placed_order.auction_id IS NOT NULL;

DELETE FROM orders
WHERE auction_id IS NOT NULL;

DELETE FROM auction_events;
DELETE FROM auction_bids;
DELETE FROM auctions;

-- Covers interrupted legacy reservations that were not linked to an auction.
UPDATE products
SET auction_reserved = FALSE
WHERE COALESCE(auction_reserved, FALSE) = TRUE;

COMMIT;
