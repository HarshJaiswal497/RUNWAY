-- V3__update_apparel_product_titles.sql
-- Updates apparel product titles to luxury editorial names
-- Uses SKU as identifier — safe for all team members regardless of product ID

-- ── WOMEN'S GOWNS ─────────────────────────────────────────────────────────
UPDATE products SET title = 'Noir Escalier Gown'
WHERE sku = 'APP-001';

UPDATE products SET title = 'Lumière Hallway Gown'
WHERE sku = 'APP-002';

UPDATE products SET title = 'Argent Couloir Gown'
WHERE sku = 'APP-003';

UPDATE products SET title = 'Obsidian Meridian Gown'
WHERE sku = 'APP-004';

UPDATE products SET title = 'Crimson Noir Evening Gown'
WHERE sku = 'APP-005';

-- ── WOMEN'S SAREE ─────────────────────────────────────────────────────────
UPDATE products SET title = 'Zari Rouge Silk Saree'
WHERE sku = 'APP-006';

-- ── MEN'S SUITS ───────────────────────────────────────────────────────────
UPDATE products SET title = 'Maison Noir Signature Suit'
WHERE sku = 'APP-007';

UPDATE products SET title = 'Or Noir Brocade Suit'
WHERE sku = 'APP-008';

UPDATE products SET title = 'Écarlate Peak Lapel Suit'
WHERE sku = 'APP-009';

UPDATE products SET title = 'Velours Cramoisi Evening Suit'
WHERE sku = 'APP-010';

UPDATE products SET title = 'Le Manteau Suit'
WHERE sku = 'APP-011';

UPDATE products SET title = 'Champagne Accent Tailored Suit'
WHERE sku = 'APP-012';

-- ── UNISEX ────────────────────────────────────────────────────────────────
UPDATE products SET title = 'Dorure Shimmer Midi Dress'
WHERE sku = 'APP-013';

UPDATE products SET title = 'Satin Lapel Unisex Suit'
WHERE sku = 'APP-014';