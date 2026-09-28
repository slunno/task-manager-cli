-- Executar via MCP/SQL Editor. Dados ficticios sao revertidos em subtransacao.
-- Se qualquer verificacao falhar, o erro nao e ocultado.
DO $verificacao$
DECLARE
  pessoa BIGINT;
  categoria BIGINT;
  chamado BIGINT;
  historico BIGINT;
  bloqueado BOOLEAN;
BEGIN
  BEGIN
    SELECT id INTO STRICT categoria FROM helpdesk.categorias WHERE nome = 'Outros';
    INSERT INTO helpdesk.usuarios(nome, email)
      VALUES ('Teste transacional', 'verificacao-supabase@exemplo.invalid') RETURNING id INTO pessoa;
    INSERT INTO helpdesk.chamados(numero, titulo, descricao, solicitante_id, categoria_id)
      VALUES ('VERIFICACAO-SUPABASE', 'Teste transacional', 'Dado ficticio a reverter', pessoa, categoria)
      RETURNING id INTO chamado;
    INSERT INTO helpdesk.historico_chamado(chamado_id, usuario_id, campo, valor_novo)
      VALUES (chamado, pessoa, 'status', 'ABERTO') RETURNING id INTO historico;

    bloqueado := false;
    BEGIN
      UPDATE helpdesk.historico_chamado SET valor_novo = 'FECHADO' WHERE id = historico;
    EXCEPTION WHEN insufficient_privilege THEN bloqueado := true;
    END;
    IF NOT bloqueado THEN RAISE EXCEPTION 'UPDATE de historico deveria ser bloqueado'; END IF;

    bloqueado := false;
    BEGIN
      DELETE FROM helpdesk.historico_chamado WHERE id = historico;
    EXCEPTION WHEN insufficient_privilege THEN bloqueado := true;
    END;
    IF NOT bloqueado THEN RAISE EXCEPTION 'DELETE avulso de historico deveria ser bloqueado'; END IF;

    PERFORM set_config('helpdesk.retencao', 'on', true);
    DELETE FROM helpdesk.historico_chamado WHERE id = historico;
    IF NOT FOUND THEN RAISE EXCEPTION 'Retencao nao excluiu o historico de teste'; END IF;
    PERFORM set_config('helpdesk.retencao', 'off', true);
    -- Excecao especifica desfaz somente esta subtransacao, inclusive a fixture.
    RAISE EXCEPTION 'Verificacao concluida; reverter fixture' USING ERRCODE = 'P9001';
  EXCEPTION WHEN SQLSTATE 'P9001' THEN NULL;
  END;
END;
$verificacao$;

SELECT
  (SELECT count(*) FROM pg_tables WHERE schemaname = 'helpdesk') AS tabelas,
  (SELECT count(*) FROM pg_tables WHERE schemaname = 'helpdesk' AND rowsecurity) AS tabelas_com_rls,
  (SELECT count(*) FROM helpdesk.flyway_schema_history WHERE success) AS migrations,
  (SELECT count(*) FROM helpdesk.usuarios WHERE email = 'verificacao-supabase@exemplo.invalid') AS fixtures_usuario,
  (SELECT count(*) FROM helpdesk.chamados WHERE numero = 'VERIFICACAO-SUPABASE') AS fixtures_chamado;
