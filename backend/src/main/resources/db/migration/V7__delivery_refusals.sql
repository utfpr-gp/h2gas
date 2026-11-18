-- HU07: recusa de entrega pelo entregador (o pedido volta ao fornecedor, sem cancelar)
CREATE TABLE delivery_refusals (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id VARCHAR(255) NOT NULL,
  deliverer_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_delivery_refusals_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_delivery_refusals_deliverer FOREIGN KEY (deliverer_id) REFERENCES deliverers(id),
  INDEX idx_delivery_refusals_deliverer_created (deliverer_id, created_at)
);
