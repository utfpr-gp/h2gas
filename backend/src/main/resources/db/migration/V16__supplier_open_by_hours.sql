-- HU18: a loja passa a estar aberta só dentro do horário de funcionamento (calculado na hora de Brasília).
-- O interruptor manual (PATCH /api/suppliers/{id}/status) saiu, e a coluna que ele gravava não é mais lida.
ALTER TABLE suppliers DROP COLUMN `open`;
