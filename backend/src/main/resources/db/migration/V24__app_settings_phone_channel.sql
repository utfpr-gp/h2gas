-- HU26 (revisão): a mensagem pode ir por WhatsApp ou SMS; as chaves passam a ter nome neutro
UPDATE app_settings SET setting_key = 'PHONE_LOGIN_CODE' WHERE setting_key = 'WHATSAPP_LOGIN_CODE';
UPDATE app_settings SET setting_key = 'PHONE_ORDER_OUT_FOR_DELIVERY' WHERE setting_key = 'WHATSAPP_ORDER_OUT_FOR_DELIVERY';
-- desligado = WhatsApp; ligado = SMS
INSERT INTO app_settings (setting_key, enabled) VALUES ('PHONE_USE_SMS', FALSE);
