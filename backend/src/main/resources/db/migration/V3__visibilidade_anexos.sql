ALTER TABLE anexos ADD COLUMN interno BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX ix_anexos_chamado_publicos ON anexos (chamado_id, criado_em, id) WHERE interno = FALSE;
