-- HU11: código numérico temporário para login por e-mail (um por usuário)
CREATE TABLE login_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(6) NOT NULL,
  user_id VARCHAR(255) NOT NULL UNIQUE,
  expiry_date DATETIME(6) NOT NULL,
  used BIT NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_login_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
