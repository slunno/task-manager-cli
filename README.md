# Portal de chamados internos de TI

Monorepo do helpdesk em português para uma organização de 200 a 500 pessoas. A entrega segue as etapas E0–E11 do prompt de produto. **Estado atual: E0, fundação.** A tela inicial e o backend sob autenticação são a base técnica; login e chamados entram nas próximas etapas.

## Estrutura

- `backend/`: Java 21, Spring Boot 3.5, Maven, PostgreSQL, Flyway e Spring Security.
- `frontend/`: React 18, TypeScript, Vite, React Router, TanStack Query e Tailwind.
- `docs/`: decisões de arquitetura, contrato de API e runbook.
- `legacy/task-manager-cli/`: código original de tarefas preservado, fora dos builds do helpdesk.

## Começar

Consulte [o runbook](docs/runbook.md) para variáveis de ambiente, Compose e comandos de build. Há um `.env.example` sem credenciais. O `.env` existente no workspace foi preservado e não é versionado.

## Decisões

Os ADRs iniciais estão em [docs/adr](docs/adr). O [plano de entrega](docs/arquitetura.md) registra módulos, riscos e suposições. A API será documentada em [docs/api.md](docs/api.md).

## Estado da E0

- Backend com configuração por ambiente, segurança fechada por padrão, Actuator e migration inicial.
- Frontend responsivo de fundação, sem simular funcionalidades não implementadas.
- Docker Compose para app, PostgreSQL, MinIO e MailHog; CI para build, lint e testes.
- Etapa seguinte: E1, identidade OIDC, provisionamento e guardas por perfil.
