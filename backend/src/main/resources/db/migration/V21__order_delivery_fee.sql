-- HU17: o pedido guarda o frete que foi cobrado e a distância usada; pedidos antigos ficam com frete 0
ALTER TABLE orders
  ADD COLUMN delivery_fee         DECIMAL(10,2) NOT NULL DEFAULT 0,
  ADD COLUMN delivery_distance_km DECIMAL(6,2)  NULL;
