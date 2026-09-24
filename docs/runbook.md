# Runbook inicial

## Subir localmente

1. Instale Java 21, Node 24 e Docker Compose.
2. Copie `.env.example` para `.env` em um ambiente novo. Defina `HELPDESK_DB_PASSWORD`, `MINIO_ROOT_USER` e `MINIO_ROOT_PASSWORD` com valores locais próprios. Se já houver `.env` de outro projeto, preserve-o e forneça as variáveis no ambiente do terminal.
3. Execute `docker compose up --build`.
4. Acesse o frontend em `http://localhost:3000`, a saúde do backend em `http://localhost:8080/actuator/health`, o MailHog em `http://localhost:8025` e o console MinIO em `http://localhost:9001`.

Na E0 o portal mostra somente a tela de fundação; não há login ou abertura de chamados. O backend aplica a migration V1 no PostgreSQL. A configuração OIDC só será ligada na E1.

## Builds independentes

- Backend: `cd backend && ./mvnw verify` (Windows: `mvnw.cmd verify`).
- Frontend: `cd frontend && npm ci && npm run lint && npm test && npm run build`.

## Banco de dados

O volume `postgres_data` guarda dados locais. Para backup em ambiente real, execute `pg_dump` em formato customizado e salve-o em destino protegido. Teste a restauração periodicamente em banco isolado com `pg_restore` antes de liberar escrita. Não use `docker compose down -v` em ambiente com dados a preservar.

## Saúde e diagnóstico

`/actuator/health` cobre liveness/readiness do processo. Métricas e logs estruturados de negócio serão adicionados nas etapas com fluxos reais. Não registre conteúdo de chamados, anexos ou dados pessoais em logs.
