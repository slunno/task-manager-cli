# Auditoria e correções do helpdesk

## C0 — Linha de base (28/09/2026)

Revisão inicial: `76b89ac`. Backend: `mvnw.cmd -B verify` aprovado, 46 testes, zero falhas/erros e dois testes PostgreSQL ignorados localmente por ausência de Docker. Frontend: `npm ci`, lint, 10 testes e build aprovados. A primeira instalação retornou EPERM porque os servidores Vite mantinham o esbuild aberto; os processos deste projeto foram encerrados e a instalação repetida, sem alterar ou desativar testes. O lockfile não mudou.

### Problemas confirmados

| Item | Evidência inicial |
| --- | --- |
| A1 | Oito arquivos `.idea/` ainda estavam no índice apesar do ignore. |
| A2 | Perfil prod não selecionava S3; o storage local tinha `matchIfMissing=true`. |
| A3 | SMTP só configurava host e porta. |
| A4 | Query/job cobriam apenas resolução na próxima hora e exigiam responsável. |
| A5 | Sem JaCoCo/Playwright; Spotless ativo, referência IDE a Checkstyle sem configuração de build. |
| M1/M2 | Comentário sem responsável não tinha destinatário; criação notificava toda TI ativa. |
| M3 | Validador limitado a PDF, PNG, JPEG e TXT. |
| M4 | Dashboard sem período/categorias/% SLA e média em horas corridas. |
| M5 | Envio por `SimpleMailMessage`, sem HTML. |
| M6 | Histórico protegido apenas por mapeamento JPA, sem trigger. |
| M7 | nginx sem cabeçalhos de segurança explícitos. |

Já atendido: downloads usam `Content-Disposition: attachment` e `X-Content-Type-Options: nosniff`; `.env` e variantes estão ignorados e somente `.env.example` é versionado. Nenhuma autorização será relaxada.

## Plano

C1 higiene; C2 configurações de produção e nginx; C3 notificações/SLA; C4 anexos; C5 dashboard; C6 histórico no banco; C7 cobertura/E2E/testes de interface; C8 documentação e fechamento. Cada etapa terá commit próprio e evidência de validação. V1–V8 e `legacy/` serão preservados.

## C1 — Higiene

A inspeção de `.idea/dataSources.xml` encontrou uma URL de conexão externa, sem usuário/senha ou credencial embutida identificável no arquivo. Nenhum valor da conexão é reproduzido aqui. Os arquivos da IDE permanecem no disco do dono e serão removidos somente do índice; o histórico Git será preservado. Caso exista credencial associada em armazenamento externo da IDE ou que tenha sido commitada antes, cabe ao dono rotacioná-la. A inspeção deste arquivo atual não demonstrou exposição de senha.

Validação C1: `git ls-files .idea` vazio; `git ls-files '.env*'` retorna apenas `.env.example`; `Test-Path .idea/dataSources.xml` permanece verdadeiro. Código e dependências intactos; a linha de base C0 continua válida.

## Etapas seguintes

Resultados, decisões, limitações e passos manuais serão registrados nas seções seguintes conforme cada etapa for validada.

## C2 — Produção

Configuração S3 obrigatória no perfil prod, sem fallback local; validação de campos não vazios com mensagens que identificam a variável, sem expor valores. SMTP configurável com padrões MailHog em dev e autenticação/STARTTLS obrigatório em prod; timeouts adicionados. CSP, nosniff, Referrer-Policy, Permissions-Policy e HSTS condicionado a HTTPS no nginx. Se houver terminação TLS externa, HSTS fica nesse proxy.

Validação: nove testes de contexto isolado carregam os arquivos reais de configuração e verificam ausência de cada variável, seleção S3, rejeição local/TLS e compatibilidade dev; build de produção do frontend aprovado. Inicialização não testa conexão remota ao bucket/SMTP; upload/download e nginx em execução serão verificados no Compose da C7. Docker não está instalado localmente, sem alterar essa limitação inicial.

## C3 — Notificações e SLA

Consultas de resolução incluem chamados sem responsável; consulta separada de primeira resposta exige ausência de resposta, prazo na próxima hora e SLA ativo. Destinatários da TI são configuráveis (`usuarios` como padrão ou `lista` de caixas). O mesmo mecanismo atende criação, reabertura, comentários do solicitante sem responsável e alertas. Responsável inativo também encaminha alerta à TI. Deduplicação inclui chamado/tipo/instante e identificador do destinatário, necessário quando houver várias caixas.

Envio MIME com HTML escapado e texto alternativo, status, número e link; resolução aponta para `#avaliacao`. Nenhuma descrição, solução ou nota interna entra no payload; alertas SLA usam título genérico. Não foi adicionada biblioteca de template: o escape usa `HtmlUtils` do Spring. A entrega SMTP mantém a semântica documentada de pelo menos uma tentativa, podendo repetir se houver queda após envio e antes do commit; a deduplicação impede reenfileirar a cada execução do alerta, sem prometer exatamente uma entrega SMTP.

Validação: testes com relógio fixo para ambos os prazos, sem responsável, configuração de destinatários, execuções consecutivas sem duplicatas, MIME HTML/texto e ausência de vazamento; consulta de banco testa chamados respondidos, pausados e concluídos.

## C4 — Anexos

Adicionados GIF, WebP, CSV, LOG, DOCX, XLSX e ZIP com validação de conteúdo e MIME, mantendo os formatos antigos. Allowlist configurável só aceita tipos suportados. Texto usa UTF-8 e restrição de caracteres de controle; não há assinatura binária para CSV/LOG. ZIP valida diretório central, limites de 1.000 entradas/20 MB por entrada/50 MB total/proporção de 100 vezes acima de 1 MB, caminhos perigosos, nomes/extensões de scripts/executáveis e compactados conhecidos, além de symlinks. Office verifica componentes obrigatórios e tipo principal, sem DTD/entidades externas. A decisão conservadora bloqueia Office dentro de ZIP e macros identificáveis.

`ScannerAnexo` é chamado antes da gravação; uma implementação externa pode rejeitar. O padrão não faz antivírus, conforme escopo. Downloads mantêm attachment/nosniff. Testes cobrem cada novo formato válido/inválido na API, retorno 400, download idêntico, ZIP malicioso e scanner antes do storage. Nenhuma dependência adicional foi necessária.

## C5 — Dashboard

Agregações no banco com período inclusivo por criação, 30 dias por padrão, categorias e percentual de resolvidos dentro do prazo. Média calculada sobre minutos úteis persistidos ao resolver, reutilizando CalendarioUtil; reabertura limpa a medição. Registros antigos sem medição não recebem estimativa falsa e são informados na resposta/tela. ADR 0008 registra a decisão e inclusão da espera pelo solicitante.

V9 é usada pela C5 e V10 será usada pelo histórico da C6, corrigindo o conflito de numeração do prompt sem alterar V1–V8. Testes controlados incluem fim de semana, feriado, SLA dentro/fora e chamado fora do período; teste PostgreSQL de 100 mil chamados também mede p95 do dashboard (<300 ms). A execução PostgreSQL é feita na CI, pois não há Docker local. Contrato e cliente regenerados; testes de tela verificam métricas, período e erro.

## C6 — Histórico no PostgreSQL

V10 bloqueia UPDATE e DELETE do histórico; exclusão só no contexto local da transação da retenção. RetencaoService exige autocommit desligado no PostgreSQL e desativa o contexto antes de retornar, sem ativação global. ADR 0007 descreve a garantia e seu limite contra administradores do banco. H2 mantém os testes funcionais e não simula o trigger.

HistoricoPostgresTest executa no PostgreSQL real da CI: operações avulsas rejeitadas, UPDATE rejeitado mesmo com contexto, rollback sem perda, retenção anonimiza/apaga e DELETE posterior continua bloqueado. V1–V8 preservadas. A CI da C5 também aprovou o p95 de fila e dashboard com 100 mil chamados.

## C7 — Qualidade

JaCoCo 0.8.15 instalado por necessidade de medição/gate: resultado inicial 85,16% (1.395/1.638 linhas) no conjunto domain/application; gate LINE de 80% aplicado ao mesmo escopo, sem exclusão de classes do negócio e sem redução do mínimo. Relatório por pacote e plano de melhoria em docs/qualidade.md. Spotless permanece obrigatório; referências órfãs de configuração da IDE foram retiradas na C1, sem adotar Checkstyle adicional.

Os dez testes originais do frontend foram preservados por funcionalidade. Novos testes de dashboard/admin/relatórios/fila/detalhe elevam a suíte a 19. Playwright 1.63.0 foi adicionado somente em desenvolvimento, com quatro fluxos serializados no Compose real, upload/download, CSRF, headers nginx e entrega HTML/texto no MailHog. CI publica relatório/capturas e cobre PostgreSQL sem skips. A avaliação ocorre após a nova resolução, preservando a regra existente.

A primeira CI da C6 detectou uma asserção que buscava o texto no wrapper Spring; o trigger já bloqueava a operação. O teste foi corrigido para verificar a causa PostgreSQL no commit e640103; CI aprovada sem alterar o trigger.

## C8 — Fechamento e pendências operacionais

### Itens tratados

| Item | Resultado | Evidência principal |
| --- | --- | --- |
| A1 | .idea retirada do índice, mantida no disco; somente .env.example versionado | C1 e verificação final do índice |
| A2 | Prod exige S3; configuração ausente e override local são rejeitados | ConfiguracaoProducaoTest |
| A3 | SMTP com credenciais/auth/STARTTLS obrigatório em prod e defaults MailHog em dev | Testes de contexto; variáveis no runbook/.env.example |
| A4 | Alertas para primeira resposta/resolução, inclusive sem responsável ativo | Clock fixo, consultas de banco e deduplicação |
| A5 | JaCoCo, gate 80%, frontend organizado e E2E real no Compose | docs/qualidade.md e CI |
| M1/M2 | Destino configurável de TI, inclusive comentário sem responsável | DestinatariosTi e testes de eventos |
| M3 | Novos formatos, limites ZIP/Office, allowlist e interface ScannerAnexo | Validador, testes de tipos e download |
| M4 | % SLA, categorias, horas úteis incrementais e período | V9, ADR 0008, teste controlado e p95 com 100 mil |
| M5 | MIME HTML escapado + alternativa texto, sem descrição/solução/notas | Teste MIME e MailHog no E2E |
| M6 | Trigger rejeita alteração/exclusão avulsa; retenção transacional preservada | V10, ADR 0007, PostgreSQL na CI |
| M7 | CSP, nosniff único no proxy, Referrer/Permissions e HSTS somente HTTPS | nginx de produção no Compose e E2E |

### Decisões e limites

- ADR [0007](adr/0007-historico-imutavel.md): histórico imutável no banco com exclusão limitada ao contexto transacional da retenção; não protege contra administrador SQL com privilégios capazes de contornar o trigger.
- ADR [0008](adr/0008-dashboard-horas-uteis.md): cálculo incremental com CalendarioUtil. Período usa **criação** do chamado, média inclui espera durante expediente e usa a última resolução após reabertura.
- V9 ficou com o dashboard da C5 e V10 com histórico da C6 para respeitar a ordem de execução. Nenhuma migration V1–V8 foi editada; legacy permaneceu intacto.
- Resolvidos antigos sem medição útil são excluídos somente da média e contados explicitamente. Não foi reconstruído um calendário histórico que não existe.
- Cobertura inicial local: 85,16%; com PostgreSQL na CI: 85,71% (1.404/1.638 linhas). Gate de 80% no conjunto domain/application; cobertura por pacote/ramo tem lacunas registradas em qualidade.md.
- Spotless continua como gate Java; configuração órfã da IDE retirada. Não foi acrescentado Checkstyle redundante.
- A primeira execução E2E detectou nosniff duplicado entre backend/nginx; o proxy passou a emitir uma única cópia, sem alterar a proteção do backend direto. O teste de reabertura foi ajustado para ABERTO/sem responsável, seguido de nova ação de assumir, conforme a regra de domínio existente. A seleção da nota passou a usar o nome acessível do combobox confirmado na captura, sem alteração da regra de avaliação.
- Prévia em memória permanece uma demonstração parcial; auditoria e E2E usam backend real. Recursos externos e segurança não são validados por essa prévia.

### Não executado e passos do dono

1. **H1 — renomeio GitHub:** ação manual do dono. Documentação usa links relativos e comandos partindo da raiz. Após renomear, atualizar origin, integrações e favoritos; manter a pasta real legacy/task-manager-cli.
2. **Credenciais:** a inspeção atual de dataSources.xml mostrou URL externa, sem usuário/senha ou credencial embutida identificável; nenhum valor foi reproduzido. O arquivo permanece no histórico, que não foi reescrito. Rotacionar qualquer credencial que o dono saiba ter sido exposta anteriormente/fora desse arquivo.
3. **Produção:** fornecer segredos OIDC/banco/S3/SMTP, bucket privado e TLS no proxy, revisar domínio de identidade e destinatários TI. Validar login real, upload/download S3, SMTP com TLS e HSTS no endpoint HTTPS. Os testes de contexto confirmam configuração/seleção do bean, sem provar conectividade remota.
4. **Operação:** separar dono das migrations e usuário da aplicação, configurar backups protegidos e realizar restauração isolada; aprovar o prazo de retenção com o responsável por dados.
5. **Antivírus:** ScannerAnexo padrão não escaneia malware; integração ClamAV ficou fora de escopo por solicitação expressa. ZIP/Office têm validação estrutural e limites. A identificação de entradas perigosas usa nomes/extensões e Content Types; arquivos renomeados podem exigir classificação por scanner. Não há garantia de ausência de malware.
6. **Entrega SMTP:** outbox evita reenfileirar o mesmo alerta, mas uma queda após envio e antes de confirmar pode repetir a entrega. Validar monitoramento operacional; não foi prometido exatamente uma vez.

Nenhuma funcionalidade da seção “fora de escopo” foi acrescentada: e-mail de entrada, sincronização de diretório, notificações internas, ações em massa, observadores/menções, busca completa, escalonamento, relatórios por agente, WhatsApp e novas plataformas de observabilidade/deploy continuam backlog, sem compromisso nesta correção.

### Validação de encerramento

Em 28/09/2026, a revisão de código e3c6459 passou na execução CI 36420445251: backend **80 testes, zero falhas/erros/skips**, gate e Spotless aprovados; frontend **19 testes**, lint, contrato gerado, build e audit aprovados; **4 E2E aprovados em 32,2 s** no Compose real. Relatórios JaCoCo e Playwright publicados. Os testes de desempenho mantiveram o p95 de fila/dashboard abaixo de 300 ms com 100 mil chamados no runner; isso não garante a mesma latência em produção.

O fechamento C8 altera documentação, preservando o código validado na C7. README, API, runbook, segurança, matriz de autorização, qualidade e este relatório foram revisados; links locais e diff verificados. Os commits C0–C7 e os ajustes de verificação permanecem no histórico normal. O commit C8 registra a conclusão e os passos manuais acima.
## Segunda rodada

### R1 — Superfície do proxy

Confirmado o encaminhamento público de Actuator e Swagger. O nginx agora publica somente `/actuator/health` e seus subcaminhos; demais rotas Actuator, API docs e Swagger retornam 404 sem encaminhamento nem fallback da SPA. Um cenário Playwright verifica health e os bloqueios usando o nginx real do Compose. A execução local do E2E depende de Docker; os testes e o build de referência são registrados junto às evidências desta rodada. A branch `codex/c7-validacao` não foi mesclada.

Linha de base: backend `mvnw.cmd -B verify` aprovado (91 testes, cinco PostgreSQL omitidos sem Docker). Frontend `npm ci`, lint e build aprovados; dois testes não encontraram elementos assíncronos na execução simultânea com o backend. Repetição isolada da mesma suíte: 20/20 aprovados, sem mudanças nos testes para ocultar as falhas.

### R2 — Contêineres e saúde

Confirmados nginx root e ausência de healthchecks dos serviços da aplicação. Frontend passa a usar nginx-unprivileged e porta interna 8080, preservando localhost:3000. Backend e frontend sem novas permissões/capabilities; healthchecks verificam a API e seu proxy, e MailHog responde por HTTP. Dependências aguardam serviços saudáveis. Curl é incluído na imagem Java exclusivamente para o healthcheck. MinIO permanece opcional, fora da dependência do perfil dev, sem nova sonda nesta rodada. O script de espera E2E continua como diagnóstico adicional; validação real dos contêineres fica na CI por ausência de Docker local.

### R3 — CSP

Confirmado `unsafe-inline` somente para estilos. Sem usos de estilos inline no código atual, removida a exceção e documentado o CSS estático de Tailwind. E2E verifica a política e erros de CSP no atendimento. Build e testes do frontend permanecem obrigatórios; evidência no navegador real depende do job Compose da CI. Não foram alteradas as regras de scripts ou relaxados os demais cabeçalhos.

### R4 — Limitação de taxa

Confirmada ausência de limites. Adicionado filtro em memória com janela fixa e chaves limitadas, após CSRF e antes dos handlers de login OIDC. Login usa IP; mutações sensíveis usam usuário autenticado. Recusa 429 Problem Details/Retry-After, parâmetros configuráveis e perfil test desligado. Testes dedicados verificam cotas, usuários distintos, expiração, capacidade e concorrência. Substituído tratamento genérico de forwarded headers pela confiança explícita do Tomcat, vazia por padrão; nginx não preserva X-Forwarded-For recebido do visitante. Não foram criadas dependências, endpoints ou mudanças de contratos OpenAPI. O limite por instância e as fronteiras da janela ficam documentados.

### R5 — Dependências e análise de segurança

Confirmada ausência das automações. Dependabot semanal e agrupado cobre Maven, npm, Actions e Docker. Workflow separado executa CodeQL Java/Kotlin com build explícito e JavaScript/TypeScript sem build, com permissões mínimas; Trivy constrói e analisa as duas imagens, publicando relatórios HIGH/CRITICAL inclusive sem correção disponível. Trivy é consultivo durante a triagem inicial, com status de falha e artefatos preservados, sem exclusões silenciosas ou alegação de ausência de vulnerabilidades. Torná-lo bloqueante exige tratar os resultados reais; CodeQL não foi tornado consultivo por antecipação. Não há credenciais de runtime nos builds.

### R6 — Cobertura de ramos e regras críticas

Medidos 58,72% de ramos antes de alterar o gate, adotado mínimo conservador de 55% e mantidos 80% de linhas. Novos testes cobrem conflito/desativação/vínculo de setores, validação/edição de avisos, privacidade e autoria de artigos, domínio e inatividade OIDC e impedimento de elevação de perfil por claims. A confiança de forwarded headers é verificada na valve real do Tomcat com requisições simuladas. O primeiro teste com servidor HTTP local falhou por socket Unix do JDK Windows; substituído pela verificação da valve, sem desabilitar a proteção nem o teste de confiança. Verify final aprovado com 110 testes (cinco PostgreSQL omitidos localmente), 88,10% de linhas e 60,89% de ramos. Não foram alteradas classes de negócio para aumentar números nem excluídos pacotes do gate.

### R7 — Higiene remota e ações do dono

Confirmado por `git rev-list --left-right --count origin/master...origin/codex/c7-validacao` que a branch antiga está atrás de master e não tem commits exclusivos. Não foi mesclada, apagada nem usada como base. O dono pode removê-la após conferir que não há trabalho pendente; utilizar seu estado antigo como base de uma entrega pode reintroduzir código/documentação desatualizados. Permanecem ações manuais: renomear o repositório e atualizar integrações/remotes; conferir Actions no último commit; rotacionar credenciais que tenham sido expostas. Nenhum histórico foi reescrito e `legacy/` e migrations V1–V10 foram preservados.

### R8 — Fechamento, decisões e riscos restantes

Atualizados segurança, runbook, qualidade, README e `.env.example`. CI agora verifica também o UID dos contêineres. R1–R5 tiveram CI de backend/frontend/E2E aprovada; CodeQL Java/TypeScript executou com sucesso. Relatórios Trivy foram gerados e examinados: 38 HIGH no frontend e quatro CRITICAL/duas HIGH no backend, sem supressões. A remediação desses achados é um incremento posterior, não um falso positivo presumido. Trivy permanece consultivo e a necessidade de triagem está explícita em segurança. Corrigida a política Dependabot para evitar migração automática major de Spring Boot/Springdoc após erro real na primeira execução.

Não implementados: healthcheck do MinIO opcional, proteção de taxa distribuída, cobertura exaustiva de todas as lacunas e itens fora de escopo do prompt. Motivos: MinIO não é dependência dev; taxa foi solicitada por instância; testes priorizam regras de risco sem inflar métricas; funcionalidades de backlog permanecem fora desta rodada. SMTP de produção segue exigindo autenticação e STARTTLS; nenhuma exceção para relay interno foi adicionada. OpenAPI não mudou, pois não foram adicionados endpoints ou DTOs nesta rodada. Os arquivos de ambiente locais não foram incluídos em commits.
