-- F12: código público do produto na URL da edição, no lugar do id sequencial
ALTER TABLE product_items ADD COLUMN public_id VARCHAR(12) NULL;

UPDATE product_items
SET public_id = SUBSTRING(MD5(CONCAT(id, RAND(), UUID())), 1, 10)
WHERE public_id IS NULL;

ALTER TABLE product_items MODIFY public_id VARCHAR(12) NOT NULL;
ALTER TABLE product_items ADD CONSTRAINT uk_product_items_public_id UNIQUE (public_id);
