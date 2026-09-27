ALTER TABLE notificacoes_outbox ADD COLUMN dedup_key VARCHAR(180);
CREATE UNIQUE INDEX ux_notificacoes_dedup_key ON notificacoes_outbox (dedup_key) WHERE dedup_key IS NOT NULL;