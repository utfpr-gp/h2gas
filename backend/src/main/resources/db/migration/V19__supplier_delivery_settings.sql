-- HU17: como a loja entrega. Os padrões mantêm o comportamento de antes: entrega e retirada, sem raio e sem frete.
ALTER TABLE suppliers
  ADD COLUMN offers_delivery     BIT(1)        NOT NULL DEFAULT 1,
  ADD COLUMN offers_pickup       BIT(1)        NOT NULL DEFAULT 1,
  ADD COLUMN delivery_radius_km  DECIMAL(6,2)  NULL,
  ADD COLUMN delivery_base_fee   DECIMAL(10,2) NOT NULL DEFAULT 0,
  ADD COLUMN delivery_fee_per_km DECIMAL(10,2) NOT NULL DEFAULT 0;
