# ADR 0007 — Histórico imutável com exceção transacional de retenção

Status: aceito (C6).

## Decisão

A V10 instala trigger PostgreSQL BEFORE UPDATE OR DELETE em historico_chamado. UPDATE é sempre rejeitado. DELETE só passa quando `current_setting('helpdesk.retencao', true) = 'on'`. Inserts continuam permitidos. A V9 foi necessária para a medição do dashboard na C5; V1–V8 permanecem intactas.

RetencaoService habilita o contexto com `set_config(..., true)` na conexão da sua transação, exige autocommit desligado, anonimiza somente FECHADOS anteriores ao limite configurado e desabilita o contexto após o lote. O parâmetro local não sobrevive a commit/rollback. H2 dos testes de unidade não tem o trigger; a proteção é validada adicionalmente no PostgreSQL real da CI.

## Limites

Essa proteção impede alterações SQL acidentais e preserva o caminho LGPD. Não constitui uma barreira contra um operador com credenciais do banco: ele pode ativar a configuração, truncar a tabela ou desabilitar o trigger com privilégios suficientes. A aplicação não expõe SQL nem esse parâmetro por API. Produção deve separar o dono de migrations do usuário da aplicação e restringir acesso administrativo. Backup, papel dedicado para retenção e auditoria de operadores continuam responsabilidades operacionais.

## Evidência

HistoricoPostgresTest cobre UPDATE/DELETE avulsos, UPDATE mesmo no contexto, rollback sem perda, anonimização/exclusão legítima, desativação antes de retornar e DELETE posterior rejeitado. RetencaoServiceTest conserva os casos de limpeza e nova tentativa de storage.
