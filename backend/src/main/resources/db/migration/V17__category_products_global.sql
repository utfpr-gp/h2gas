-- HU19: as categorias (P13, P45, Galão 20L…) passam a ser globais, cadastradas só pelo admin.
-- Cada fornecedor tinha as suas; categorias iguais (mesmo tipo e nome) viram uma só, e os produtos são
-- repostos nela — nenhum produto perde a categoria. Depois disso a coluna supplier_id sai.

-- 1) nomes sem espaços nas pontas. O agrupamento abaixo segue a collation da coluna (a mesma do UNIQUE do
--    passo 6: no MySQL padrão ela não diferencia caixa nem acento), então o que agrupa aqui não conflita lá.
UPDATE category_products SET name = TRIM(name);

-- 2) a canônica de cada grupo (tipo + nome) é a de menor id
CREATE TEMPORARY TABLE category_canonical AS
SELECT c.id AS dup_id, m.canon_id
FROM category_products c
JOIN (SELECT product_type_id, name, MIN(id) AS canon_id
      FROM category_products
      GROUP BY product_type_id, name) m
  ON m.product_type_id <=> c.product_type_id AND m.name = c.name;

-- 3) produtos das duplicadas passam para a canônica
UPDATE product_items pi
JOIN category_canonical cc ON pi.category_product_id = cc.dup_id
SET pi.category_product_id = cc.canon_id
WHERE cc.dup_id <> cc.canon_id;

-- 4) apaga as duplicadas (já sem produtos)
DELETE c FROM category_products c
JOIN category_canonical cc ON cc.dup_id = c.id
WHERE cc.dup_id <> cc.canon_id;

DROP TEMPORARY TABLE category_canonical;

-- 5) a categoria deixa de ter loja: solta a FK (nome gerado pelo Hibernate, descoberto no catálogo) e a coluna
SET @fk := (
  SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'category_products'
    AND COLUMN_NAME = 'supplier_id' AND REFERENCED_TABLE_NAME IS NOT NULL
  LIMIT 1
);
SET @sql := IF(@fk IS NOT NULL,
  CONCAT('ALTER TABLE category_products DROP FOREIGN KEY `', @fk, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE category_products DROP COLUMN supplier_id;

-- 6) nome único por tipo
ALTER TABLE category_products
  ADD CONSTRAINT uq_category_products_type_name UNIQUE (product_type_id, name);
