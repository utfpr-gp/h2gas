-- F7 fatia 6: cidade atendida guarda o código de município do IBGE (o mesmo que o ViaCEP devolve no campo "ibge")
ALTER TABLE cities ADD COLUMN ibge_code INT NULL;
ALTER TABLE cities ADD CONSTRAINT uk_cities_ibge_code UNIQUE (ibge_code);
UPDATE cities SET ibge_code = 4106902 WHERE name = 'Curitiba' AND state = 'PR';
