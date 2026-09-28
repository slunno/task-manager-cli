# Qualidade e testes — C7

## Cobertura

JaCoCo 0.8.15 mede linhas e ramos das classes em `**/domain/**` e `**/application/**`, incluindo todos os módulos (66 classes na medição inicial). `verify` gera HTML/XML/CSV em `backend/target/site/jacoco` e exige **80% de linhas e 55% de ramos no conjunto desses pacotes**. Não inclui controllers, infraestrutura, DTOs externos ou código gerado de mapeamento. Os limites não se aplicam individualmente a cada pacote.

Medição anterior ao gate: **1.395/1.638 linhas = 85,16%**, suíte completa local, com cinco testes PostgreSQL omitidos automaticamente por ausência de Docker. Na CI esses testes são executados; a medição com PostgreSQL foi **1.404/1.638 linhas = 85,71%**. O limite solicitado foi mantido em 80%, sem reduzir para esconder lacunas. O relatório é publicado como artefato da CI.

Lacunas da medição inicial: admin/application (50%); gestao/application (55,6%); conhecimento/domain (61,1%); usuarios/application (74,3%); sla/application (75%). A segunda rodada acrescenta testes de renomeação/conflitos/desativação de setores, períodos inválidos e edição de avisos, despublicação/privacidade/autoria de artigos e domínio/verificação/inatividade/perfil no OIDC. Os testes de taxa cobrem concorrência, capacidade, expiração e separação de usuários; a valve real do Tomcat é exercitada com requisições simuladas para verificar confiança no proxy sem depender de sockets locais. Permanecem oportunidades de ampliar regras de vínculos e SLA, sem excluir suas classes do gate.

A linha de base de ramos foi **488/831 = 58,72%**. O gate de 55% foi escolhido antes dos novos testes, arredondando conservadoramente a medição; não foi reduzido após falhas. Testes adicionais melhoram a cobertura, mas não substituem a matriz de autorização nem demonstram ausência de defeitos.

Após R6, `mvnw.cmd -B verify` aprovado: **110 testes, zero falhas/erros, cinco PostgreSQL omitidos por ausência de Docker**. Cobertura local: **1.443/1.638 linhas = 88,10%** e **506/831 ramos = 60,89%**. O relatório completo é artefato da CI; os testes PostgreSQL são executados no runner com Docker.

## Formatação

Spotless com Google Java Format permanece como única ferramenta Java de formatação. A configuração órfã da IDE foi retirada do índice na C1. Não se adota um segundo formatador/checker que conflite com esse padrão. A verificação de formatação continua obrigatória no `verify`.

## Frontend

Os dez testes do antigo App.test.tsx foram preservados em Acesso, NovoChamadoPage, FilaTiPage, DetalheChamadoPage e InteracoesChamadoPanel. Helpers compartilhados ficam em src/test/app.tsx. Novos testes cobrem dashboard, período, filas vazias/erro/filtros, erro de detalhe, administração de categorias (criar/desativar/conflito), relatórios e exportação CSV com filtros.

## E2E real

Playwright 1.63.0 é dependência de desenvolvimento para testar navegador real. Quatro cenários encadeados e um cenário de exposição/CSP do proxy usam Chrome/Chromium contra **nginx do build de produção + Spring perfil dev + PostgreSQL + MailHog** no Docker Compose, sem mocks. Somente o agente é inserido por SQL no ambiente descartável; funcionários usam login dev normal, sem elevação de perfil pela API.

Fluxos: funcionário abre/anexa/lista; TI assume/comenta/nota interna/resolve; solicitante vê apenas conteúdo público, reabre para ABERTO/sem responsável, TI assume/resolve novamente e solicitante avalia; outro funcionário recebe página/HTTP 404 também para timeline e anexos. A nova resolução é necessária porque a regra atual só permite avaliar chamados concluídos. Também verificam CSRF, cabeçalhos de segurança e erros JavaScript. Os cenários são serializados porque representam o mesmo atendimento; uma falha interrompe o restante sem esconder o erro.

Execução local (Docker necessário): defina valores fictícios no ambiente, suba `docker compose up -d --build postgres mailhog backend frontend`, execute `node .github/scripts/wait-compose.mjs`, prepare o agente com frontend/e2e/seed.sql no psql, instale Chromium com `npx playwright install chromium` no frontend e execute `npm run test:e2e`. Use banco descartável: a seed altera apenas o usuário agente-e2e@exemplo.invalid. O job da CI fornece valores fictícios, publica HTML/evidências por sete dias e encerra os volumes próprios.

Fontes de configuração: [JaCoCo Maven](https://www.jacoco.org/jacoco/trunk/doc/maven.html), [gate JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html), [Playwright CI](https://playwright.dev/docs/ci).
