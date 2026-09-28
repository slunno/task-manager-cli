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

Adicionados GIF, WebP, CSV, LOG, DOCX, XLSX e ZIP com validação de conteúdo e MIME, mantendo os formatos antigos. Allowlist configurável só aceita tipos suportados. Texto usa UTF-8 e restrição de caracteres de controle; não há assinatura binária para CSV/LOG. ZIP valida diretório central, limites de 1.000 entradas/20 MB por entrada/50 MB total/proporção de 100 vezes acima de 1 MB, caminhos perigosos, scripts/executáveis, symlinks e compactação aninhada. Office verifica componentes obrigatórios e tipo principal, sem DTD/entidades externas. A decisão conservadora bloqueia Office dentro de ZIP e macros identificáveis.

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
5. **Antivírus:** ScannerAnexo padrão não escaneia malware; integração ClamAV ficou fora de escopo por solicitação expressa. ZIP/Office têm validação estrutural e limites, não garantia de ausência de malware.
6. **Entrega SMTP:** outbox evita reenfileirar o mesmo alerta, mas uma queda após envio e antes de confirmar pode repetir a entrega. Validar monitoramento operacional; não foi prometido exatamente uma vez.

Nenhuma funcionalidade da seção “fora de escopo” foi acrescentada: e-mail de entrada, sincronização de diretório, notificações internas, ações em massa, observadores/menções, busca completa, escalonamento, relatórios por agente, WhatsApp e novas plataformas de observabilidade/deploy continuam backlog, sem compromisso nesta correção.

### Validação de encerramento

Em 28/09/2026, a revisão de código e3c6459 passou na execução CI 36420445251: backend **80 testes, zero falhas/erros/skips**, gate e Spotless aprovados; frontend **19 testes**, lint, contrato gerado, build e audit aprovados; **4 E2E aprovados em 32,2 s** no Compose real. Relatórios JaCoCo e Playwright publicados. Os testes de desempenho mantiveram o p95 de fila/dashboard abaixo de 300 ms com 100 mil chamados no runner; isso não garante a mesma latência em produção.

O fechamento C8 altera documentação, preservando o código validado na C7. README, API, runbook, segurança, matriz de autorização, qualidade e este relatório foram revisados; links locais e diff verificados. Os commits C0–C7 e os ajustes de verificação permanecem no histórico normal. O commit C8 registra a conclusão e os passos manuais acima.
