ALTER TABLE chamados ADD COLUMN resolucao_minutos_uteis BIGINT;
ALTER TABLE chamados ADD CONSTRAINT ck_resolucao_minutos_uteis CHECK (resolucao_minutos_uteis >= 0);
