-- HU06: motivo obrigatório do cancelamento feito pelo fornecedor
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(500) NULL;
