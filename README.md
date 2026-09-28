# Portal de chamados internos de TI

Monorepo do helpdesk em português para uma organização de 200 a 500 pessoas. A entrega segue o plano E0–E11 do prompt de produto. **E0–E11 implementadas.** A ativação em produção depende da configuração do SSO, segredos, infraestrutura e validação operacional descritos no runbook.

A revisão C0–C8 está registrada no [relatório de auditoria](docs/auditoria-correcoes.md). Inclui configuração obrigatória de produção, alertas dos dois prazos de SLA, e-mails HTML, anexos adicionais com validação de ZIP/Office, dashboard em horas úteis, histórico protegido no PostgreSQL e testes E2E reais. Consulte [qualidade e testes](docs/qualidade.md) para cobertura, CI e execução dos cenários.

## Estrutura

- `backend/`: Java 21, Spring Boot 3.5, Maven, PostgreSQL, Flyway e Spring Security.
- `frontend/`: React 18, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod e Tailwind.
- `docs/`: decisões de arquitetura, contrato da API e runbook.
- `legacy/task-manager-cli/`: código original de tarefas preservado, fora dos builds do helpdesk.

## Começar

Consulte [o runbook](docs/runbook.md) para variáveis de ambiente, Compose, SSO e comandos de build. Há um `.env.example` sem credenciais. O `.env` existente no workspace foi preservado e não é versionado.

## Banco Supabase

O **Projeto de chamados** foi preparado com schema privado `helpdesk` e as tabelas das migrations V1–V10. Coloque a senha do banco em `SUPABASE_DB_PASSWORD` no arquivo local `.env.supabase`, ignorado pelo Git. Use `scripts/iniciar-supabase.ps1` no Windows. Consulte [o guia Supabase](docs/supabase.md) para inicializacao, tabelas, IPv6/Session pooler e isolamento.

## Entregas

- **E0:** monorepo, migration base, Compose, CI, ADRs e documentação.
- **E1:** login OIDC corporativo em `prod`, login simulado restrito a `dev`, sessão no servidor, CSRF, provisionamento no primeiro login, perfis e bloqueio de inativos. O frontend mostra rotas por perfil e estados de carregamento/erro.
- **E2:** categorias iniciais, criação, listagem paginada e detalhe de chamados. O backend restringe o acesso do funcionário aos próprios chamados, valida as entradas e fornece o contrato OpenAPI usado pelo cliente tipado. Testes cobrem a matriz de autorização e as fronteiras modulares.
- **E3:** fila da TI com filtros e paginação, busca de pessoas, atribuição, ação de assumir, transições de status, solução obrigatória, histórico e controle de versão para conflitos. O frontend oferece a fila e a gestão no detalhe do chamado.
- **E4:** mensagens públicas, notas internas exclusivas da TI, linha do tempo pública, anexos com validação de conteúdo, armazenamento privado e download autorizado.
- **E5:** outbox transacional, envio SMTP com tentativas e coordenação dos jobs por ShedLock.
- **E6:** prazos em horas úteis, pausa do SLA, alertas e dashboard da TI.
- **E7:** gestão de usuários e importação CSV, categorias, políticas de SLA e calendário.
- **E8:** reabertura pelo solicitante, fechamento automático e avaliação do atendimento.
- **E9:** artigos pesquisáveis, rascunhos da TI, sugestões ao abrir chamado, respostas prontas e filtros pessoais na fila.
- **E10:** avisos de incidente, vínculo interno de duplicados e relatórios agregados com exportação CSV.
- **E11:** setores e vínculo de usuários para relatórios, retenção configurável, métricas operacionais, logs JSON com identificador de requisição, índices e teste de desempenho com 100 mil chamados, revisão de segurança e runbook de implantação/restauração.

A segurança é aplicada no backend. Os guardas de rota do frontend organizam a navegação, mas não substituem a autorização da API.

## Verificar a entrega

Backend: `cd backend` e `./mvnw verify` (`mvnw.cmd verify` no Windows). Frontend: `cd frontend`, `npm ci`, `npm run lint`, `npm test`, `npm run build`. Com Docker, a suíte backend executa os testes PostgreSQL/Flyway/trigger e a massa de 100 mil chamados; sem Docker, esses cinco testes são omitidos explicitamente. O `verify` exige 80% de linhas no conjunto dos pacotes domain/application e aplica Spotless. A CI também executa os quatro cenários Playwright com Compose e publica relatórios.

## Nome do repositório

O dono pode renomear o repositório no GitHub. Os links desta documentação são relativos, e os comandos partem da raiz do checkout, independente do nome da pasta. Após o renomeio, atualize a URL do remote, integrações e favoritos. O caminho `legacy/task-manager-cli/` é o nome real do código preservado e continua fora dos builds; não depende do nome externo do repositório.
