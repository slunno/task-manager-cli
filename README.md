# Portal de chamados internos de TI

Monorepo do helpdesk em português para uma organização de 200 a 500 pessoas. A entrega é incremental conforme o plano E0–E11 do prompt de produto. **Estado atual: E2, abertura e consulta de chamados.**

## Estrutura

- `backend/`: Java 21, Spring Boot 3.5, Maven, PostgreSQL, Flyway e Spring Security.
- `frontend/`: React 18, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod e Tailwind.
- `docs/`: decisões de arquitetura, contrato da API e runbook.
- `legacy/task-manager-cli/`: código original de tarefas preservado, fora dos builds do helpdesk.

## Começar

Consulte [o runbook](docs/runbook.md) para variáveis de ambiente, Compose, SSO e comandos de build. Há um `.env.example` sem credenciais. O `.env` existente no workspace foi preservado e não é versionado.

## Entregas

- **E0:** monorepo, migration base, Compose, CI, ADRs e documentação.
- **E1:** login OIDC corporativo em `prod`, login simulado restrito a `dev`, sessão no servidor, CSRF, provisionamento no primeiro login, perfis e bloqueio de inativos. O frontend mostra rotas por perfil e estados de carregamento/erro.
- **E2:** categorias iniciais, criação, listagem paginada e detalhe de chamados. O backend restringe o acesso do funcionário aos próprios chamados, valida as entradas e fornece o contrato OpenAPI usado pelo cliente tipado. Testes cobrem a matriz de autorização e as fronteiras modulares.
- **Próxima:** E3, fila e operação da equipe de TI.

A segurança é aplicada no backend. Os guardas de rota do frontend organizam a navegação, mas não substituem a autorização da API.
