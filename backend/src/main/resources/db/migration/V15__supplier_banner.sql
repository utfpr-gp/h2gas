-- F7 §5.26: banner da loja (URL no Cloudinary), exibido no topo da página de produtos da loja
ALTER TABLE suppliers ADD COLUMN banner_url VARCHAR(500) NULL;
