
-- Aplicar pelo dono do schema; nao modifica auth, storage nem public do Supabase.
REVOKE ALL ON SCHEMA helpdesk FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON ALL TABLES IN SCHEMA helpdesk FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA helpdesk FROM PUBLIC, anon, authenticated, service_role;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA helpdesk FROM PUBLIC, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA helpdesk
  REVOKE ALL ON TABLES FROM PUBLIC, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA helpdesk
  REVOKE ALL ON SEQUENCES FROM PUBLIC, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA helpdesk
  REVOKE ALL ON FUNCTIONS FROM PUBLIC, anon, authenticated, service_role;

-- Defesa adicional: o portal usa JDBC; nao existem politicas de acesso direto pelo cliente.
-- postgres/dono do schema continua acessando. Uma conta futura sem bypass precisa de politicas proprias.
DO $$
DECLARE tabela RECORD;
BEGIN
  FOR tabela IN
    SELECT tablename FROM pg_tables WHERE schemaname = 'helpdesk'
  LOOP
    EXECUTE format('ALTER TABLE helpdesk.%I ENABLE ROW LEVEL SECURITY', tabela.tablename);
  END LOOP;
END;
$$;

-- A funcao da V10 so usa operacoes do PostgreSQL; nao precisa de schemas mutaveis.
ALTER FUNCTION helpdesk.proteger_historico_chamado() SET search_path = pg_catalog;
COMMENT ON SCHEMA helpdesk IS 'Portal de chamados: acesso pelo backend JDBC; schema privado, nao expor na Data API';
