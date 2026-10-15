-- HU03: modalidade logística do pedido (entrega no local / retirada na loja)
ALTER TABLE orders ADD COLUMN delivery_mode VARCHAR(20) NOT NULL DEFAULT 'DELIVERY';
