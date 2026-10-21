-- HU12: código para o admin confirmar o e-mail novo antes da troca (um pedido por usuário)
CREATE TABLE email_change_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL UNIQUE,
  new_email VARCHAR(255) NOT NULL,
  code VARCHAR(6) NOT NULL,
  expiry_date DATETIME(6) NOT NULL,
  used BIT NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_email_change_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
