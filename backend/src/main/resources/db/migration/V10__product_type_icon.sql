ALTER TABLE product_types ADD COLUMN icon VARCHAR(40) NOT NULL DEFAULT 'category';

UPDATE product_types SET icon = 'local_fire_department'
WHERE LOWER(name) LIKE '%gás%' OR LOWER(name) LIKE '%gas%';

UPDATE product_types SET icon = 'water_drop'
WHERE LOWER(name) LIKE '%água%' OR LOWER(name) LIKE '%agua%';
