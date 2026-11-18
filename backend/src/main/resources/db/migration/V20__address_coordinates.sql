-- HU17: coordenadas do CEP, para medir a distância da loja ao cliente (frete por km)
ALTER TABLE addresses
  ADD COLUMN latitude  DECIMAL(10,7) NULL,
  ADD COLUMN longitude DECIMAL(10,7) NULL;
