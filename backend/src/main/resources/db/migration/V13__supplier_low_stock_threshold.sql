-- F7 fatia 6: estoque mínimo do fornecedor sai do localStorage e vai para o banco (8 era o padrão da tela de produtos)
ALTER TABLE suppliers ADD COLUMN low_stock_threshold INT NOT NULL DEFAULT 8;
