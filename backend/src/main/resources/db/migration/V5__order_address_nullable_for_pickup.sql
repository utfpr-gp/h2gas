-- HU05: pedido de retirada na loja (PICKUP) não tem endereço de entrega
ALTER TABLE orders MODIFY address_id BIGINT NULL;
