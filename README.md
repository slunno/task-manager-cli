# Portal de chamados internos de TI

Monorepo do helpdesk em português para uma organização de 200 a 500 pessoas. A entrega segue o plano E0–E11 do prompt de produto. **E0–E11 implementadas.** A ativação em produção depende da configuração do SSO, segredos, infraestrutura e validação operacional descritos no runbook.

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
