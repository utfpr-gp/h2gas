-- Permite o ordinal de UserTypeEnum.DELIVERER (3).
-- O CHECK legado foi gerado pelo Hibernate com ddl-auto=update (0..2).
ALTER TABLE users DROP CHECK users_chk_1;
ALTER TABLE users ADD CONSTRAINT users_chk_1 CHECK (user_type BETWEEN 0 AND 3);
