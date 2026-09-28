-- Funcao de event trigger existente e pertencente a postgres no projeto.
-- O dono continua executando; clientes da Data API nao precisam desse privilegio.
REVOKE EXECUTE ON FUNCTION public.rls_auto_enable() FROM PUBLIC, anon, authenticated;
