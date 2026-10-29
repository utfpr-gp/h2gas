-- HU24: nota do entregador e uma avaliação por pedido (a coluna rating segue sendo a nota da loja)
ALTER TABLE reviews ADD COLUMN deliverer_rating INT NULL;
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_deliverer_rating CHECK (deliverer_rating BETWEEN 1 AND 5);
ALTER TABLE reviews ADD CONSTRAINT uk_reviews_order UNIQUE (order_id);
