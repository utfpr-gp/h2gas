-- HU26: interruptores do admin; as mensagens de WhatsApp nascem desligadas (cada envio tem custo)
CREATE TABLE app_settings (
  setting_key VARCHAR(60) NOT NULL PRIMARY KEY,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at DATETIME(6) NULL
);
INSERT INTO app_settings (setting_key, enabled) VALUES
  ('WHATSAPP_LOGIN_CODE', FALSE),
  ('WHATSAPP_ORDER_OUT_FOR_DELIVERY', FALSE);
