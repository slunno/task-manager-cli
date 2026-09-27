ALTER TABLE chamados ADD COLUMN anonimizado_em TIMESTAMPTZ;
CREATE INDEX ix_chamados_retencao ON chamados (fechado_em, id) WHERE status = 'FECHADO' AND anonimizado_em IS NULL;
CREATE INDEX ix_chamados_relatorio ON chamados (criado_em DESC, categoria_id, status);
CREATE INDEX ix_usuarios_setor ON usuarios (setor_id, id);

CREATE TABLE storage_exclusao_pendente (
  chave_storage VARCHAR(255) PRIMARY KEY,
  criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
