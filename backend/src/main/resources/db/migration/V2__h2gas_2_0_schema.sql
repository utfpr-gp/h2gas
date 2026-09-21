-- 1) addresses: permitir múltiplos endereços por usuário
--    MySQL não deixa dropar o UNIQUE de user_id enquanto ele sustenta a FK para users.
--    Sequência: dropar FK → dropar unique → índice simples → recriar FK.

SET @fk := (
  SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'addresses'
    AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME = 'users'
  LIMIT 1
);
SET @sql := IF(@fk IS NOT NULL,
  CONCAT('ALTER TABLE addresses DROP FOREIGN KEY `', @fk, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx := (
  SELECT INDEX_NAME FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'addresses'
    AND COLUMN_NAME = 'user_id' AND NON_UNIQUE = 0
  LIMIT 1
);
SET @sql := IF(@idx IS NOT NULL,
  CONCAT('ALTER TABLE addresses DROP INDEX `', @idx, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx := (
  SELECT INDEX_NAME FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'addresses'
    AND COLUMN_NAME = 'user_id'
  LIMIT 1
);
SET @sql := IF(@has_idx IS NULL,
  'ALTER TABLE addresses ADD INDEX idx_addresses_user_id (user_id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_again := (
  SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'addresses'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME = 'fk_addresses_user'
  LIMIT 1
);
SET @sql := IF(@fk_again IS NULL,
  'ALTER TABLE addresses ADD CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) product_items: segregar preço por modalidade (RN01/HU10)
ALTER TABLE product_items ADD COLUMN price_delivery DECIMAL(10,2) NULL;
ALTER TABLE product_items ADD COLUMN price_pickup DECIMAL(10,2) NULL;
UPDATE product_items SET price_delivery = price, price_pickup = price;
ALTER TABLE product_items MODIFY COLUMN price_delivery DECIMAL(10,2) NOT NULL;
ALTER TABLE product_items MODIFY COLUMN price_pickup DECIMAL(10,2) NOT NULL;
ALTER TABLE product_items DROP COLUMN price;

-- 3) deliverers: novo perfil operacional (HU07)
CREATE TABLE deliverers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  supplier_id VARCHAR(255) NOT NULL,
  active BIT NOT NULL DEFAULT 1,
  vehicle_info VARCHAR(255) NULL,
  CONSTRAINT fk_deliverers_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_deliverers_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(user_id)
);

-- 4) orders: rastreabilidade logística (HU07/HU08)
ALTER TABLE orders ADD COLUMN deliverer_id BIGINT NULL;
ALTER TABLE orders ADD COLUMN change_for DECIMAL(10,2) NULL;
ALTER TABLE orders ADD COLUMN dispatched_at DATETIME(6) NULL;
ALTER TABLE orders ADD COLUMN delivered_at DATETIME(6) NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_deliverer
  FOREIGN KEY (deliverer_id) REFERENCES deliverers(id);

-- 5) reviews: avaliação pós-entrega (HU14)
CREATE TABLE reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id VARCHAR(255) NOT NULL,
  supplier_id VARCHAR(255) NOT NULL,
  deliverer_id BIGINT NULL,
  rating INT NOT NULL,
  comment TEXT NULL,
  CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_reviews_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(user_id),
  CONSTRAINT fk_reviews_deliverer FOREIGN KEY (deliverer_id) REFERENCES deliverers(id),
  CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
