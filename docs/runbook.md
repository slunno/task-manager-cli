# Runbook

## Subir localmente

1. Instale Java 21, Node 24 e Docker Compose.
2. Copie `.env.example` para `.env` em um ambiente novo. Defina `HELPDESK_DB_PASSWORD`, `MINIO_ROOT_USER` e `MINIO_ROOT_PASSWORD` com valores locais próprios. Se já houver `.env` de outro projeto, preserve-o e forneça as variáveis no ambiente do terminal.
3. Execute `docker compose up --build`.
4. Acesse o frontend em `http://localhost:3000`, a saúde do backend em `http://localhost:8080/actuator/health`, o MailHog em `http://localhost:8025` e o console MinIO em `http://localhost:9001`.
5. No Compose, o backend usa o perfil `dev`: abra o portal e informe um nome e e-mail de teste. O usuário é criado como `FUNCIONARIO` e a sessão fica em cookie HttpOnly. Use dados fictícios nesse ambiente.

As portas do Compose ficam ligadas a `127.0.0.1`, pois o perfil `dev` permite simular qualquer e-mail. **Não use esse perfil em um ambiente acessível por outras pessoas.** A migration V1 cria o esquema PostgreSQL; o backend valida o mapeamento JPA na inicialização.

### Prévia visual sem Docker

Quando Docker ou o backend Java não estiverem disponíveis, a API simulada permite navegar pelas telas já implementadas. Ela escuta apenas em `127.0.0.1`, guarda dados somente na memória e inclui dois chamados fictícios. Não representa uma validação da integração com PostgreSQL, segurança ou SSO.

Em dois terminais PowerShell, dentro de `frontend/`:

```powershell
node scripts/preview-api.mjs
```

```powershell
$env:HELPDESK_DEV_API_TARGET = 'http://127.0.0.1:8188'
node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5173 --strictPort
```

Abra `http://localhost:5173`. Para ver a fila e operar chamados na prévia, entre com `agente@exemplo.local` ou `admin@exemplo.local` e qualquer nome. Para a visão de funcionário, use `maria@exemplo.local`. Esses perfis são exclusivos da API simulada; o backend real continua provisionando FUNCIONARIO no primeiro acesso. Os chamados e o histórico criados nessa prévia desaparecem ao reiniciar a API simulada. Para validar o sistema real, use o Compose descrito acima.

## SSO em produção

1. Registre um cliente OIDC no Microsoft Entra ID ou Google Workspace. Configure a URL de retorno pública `https://SEU_DOMINIO/login/oauth2/code/corporativo` no provedor.
2. Defina `SPRING_PROFILES_ACTIVE=prod`, `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_ALLOWED_EMAIL_DOMAIN` e `OIDC_REDIRECT_URI={baseUrl}/login/oauth2/code/{registrationId}` no ambiente ou secret manager. Defina também credenciais de banco e SMTP.
3. Sirva frontend e API sob a mesma origem HTTPS. O proxy deve encaminhar `/api/`, `/oauth2/authorization/` e `/login/oauth2/code/` ao backend, com cabeçalhos de host/protocolo corretos. O proxy local em `frontend/nginx.conf` é uma referência para isso.
4. Valide em ambiente HTTPS com o provedor real: login, renovação de sessão, logout local, CSRF em POST, cookie `HttpOnly; Secure; SameSite=Lax`, usuário inativo e retorno para a rota inicial por perfil. A integração real depende das credenciais e do IdP corporativo.

Somente identidades do domínio configurado são aceitas. Se o provedor enviar `email_verified=false`, o login é rejeitado. O primeiro acesso provisiona `FUNCIONARIO`. O logout encerra a sessão do portal; a sessão no provedor SSO pode permanecer ativa.

### Primeiro administrador

Não há usuário nem senha administrativos embutidos. Depois que uma identidade autorizada fizer o primeiro login, um operador com acesso ao PostgreSQL promove **uma conta específica**, auditando a operação fora da aplicação:

```sql
BEGIN;
UPDATE usuarios SET perfil = 'TI_ADMIN', atualizado_em = now()
WHERE lower(email) = lower('admin@empresa.com') AND ativo = true;
-- Confirme que exatamente uma linha foi alterada antes do COMMIT.
COMMIT;
```

Substitua o e-mail de exemplo pelo usuário aprovado. Na E7, a administração de perfis será feita pelo `TI_ADMIN` via interface/API. Alterações de perfil ou desativação no banco passam a valer na próxima requisição da sessão existente.

## Builds independentes

- Backend: `cd backend && ./mvnw verify` (Windows: `mvnw.cmd verify`).
- Frontend: `cd frontend && npm ci && npm run lint && npm test && npm run build`.

## Banco de dados

O volume `postgres_data` guarda dados locais. Para backup em ambiente real, execute `pg_dump` em formato customizado e salve-o em destino protegido. Teste a restauração periodicamente em banco isolado com `pg_restore` antes de liberar escrita. Não use `docker compose down -v` em ambiente com dados a preservar.

## Saúde e diagnóstico

`/actuator/health` cobre liveness/readiness do processo. Métricas e logs estruturados de negócio serão adicionados nas etapas com fluxos reais. Não registre conteúdo de chamados, anexos ou dados pessoais em logs.
