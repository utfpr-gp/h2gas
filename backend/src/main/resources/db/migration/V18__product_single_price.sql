-- HU21: o produto passa a ter um preço só; o frete vem da distância (HU17).
-- O preço único é o antigo preço de retirada, que é o preço sem frete.
ALTER TABLE product_items ADD COLUMN price DECIMAL(10,2) NULL;
UPDATE product_items SET price = price_pickup;
ALTER TABLE product_items MODIFY price DECIMAL(10,2) NOT NULL;
ALTER TABLE product_items DROP COLUMN price_delivery, DROP COLUMN price_pickup;
