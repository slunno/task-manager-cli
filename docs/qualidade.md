# Qualidade e testes — C7

## Cobertura

JaCoCo 0.8.15 mede linhas das classes em `**/domain/**` e `**/application/**`, incluindo todos os módulos (66 classes na medição inicial). `verify` gera HTML/XML/CSV em `backend/target/site/jacoco` e exige **80% no conjunto desses pacotes**. Não inclui controllers, infraestrutura, DTOs externos ou código gerado de mapeamento. A métrica não afirma 80% em cada pacote isolado nem cobertura de branches.

Medição anterior ao gate: **1.395/1.638 linhas = 85,16%**, suíte completa local, com cinco testes PostgreSQL omitidos automaticamente por ausência de Docker. Na CI esses testes são executados. O limite solicitado foi mantido em 80%, sem reduzir para esconder lacunas. O relatório é publicado como artefato da CI.

Próximos incrementos de cobertura por pacote: admin/application (50%: edição/desativação de setores e erros); gestao/application (55,6%: conflitos e validações de vínculos); conhecimento/domain (61,1%: edição e desativação); usuarios/application (74,3%: caminhos OIDC, identidade inválida e usuário inativo); sla/application (75%: pausas/retomadas e políticas ausentes). Os números por pacote são da medição inicial, disponíveis no relatório. São um plano de melhoria, sem excluir classes reais do gate.

## Formatação

Spotless com Google Java Format permanece como única ferramenta Java de formatação. A configuração órfã da IDE foi retirada do índice na C1. Não se adota um segundo formatador/checker que conflite com esse padrão. A verificação de formatação continua obrigatória no `verify`.

## Frontend

Os dez testes do antigo App.test.tsx foram preservados em Acesso, NovoChamadoPage, FilaTiPage, DetalheChamadoPage e InteracoesChamadoPanel. Helpers compartilhados ficam em src/test/app.tsx. Novos testes cobrem dashboard, período, filas vazias/erro/filtros, erro de detalhe, administração de categorias (criar/desativar/conflito), relatórios e exportação CSV com filtros.

## E2E real

Playwright 1.63.0 é dependência de desenvolvimento para testar navegador real. Quatro cenários encadeados usam Chrome/Chromium contra **nginx do build de produção + Spring perfil dev + PostgreSQL + MailHog** no Docker Compose, sem mocks. Somente o agente é inserido por SQL no ambiente descartável; funcionários usam login dev normal, sem elevação de perfil pela API.

Fluxos: funcionário abre/anexa/lista; TI assume/comenta/nota interna/resolve; solicitante vê apenas conteúdo público, reabre, TI resolve novamente e solicitante avalia; outro funcionário recebe página/HTTP 404 também para timeline e anexos. A nova resolução é necessária porque a regra atual só permite avaliar chamados concluídos. Também verificam CSRF, cabeçalhos de segurança e erros JavaScript. Os cenários são serializados porque representam o mesmo atendimento; uma falha interrompe o restante sem esconder o erro.

Execução local (Docker necessário): defina valores fictícios no ambiente, suba `docker compose up -d --build postgres mailhog backend frontend`, execute `node .github/scripts/wait-compose.mjs`, prepare o agente com frontend/e2e/seed.sql no psql, instale Chromium com `npx playwright install chromium` no frontend e execute `npm run test:e2e`. Use banco descartável: a seed altera apenas o usuário agente-e2e@exemplo.invalid. O job da CI fornece valores fictícios, publica HTML/evidências por sete dias e encerra os volumes próprios.

Fontes de configuração: [JaCoCo Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html), [gate JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html), [Playwright CI](https://playwright.dev/docs/ci).
