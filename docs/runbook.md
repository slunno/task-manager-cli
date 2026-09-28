# Runbook

## Subir localmente

1. Instale Java 21, Node 24 e Docker Compose.
2. Copie `.env.example` para `.env` em um ambiente novo. Defina `HELPDESK_DB_PASSWORD`, `MINIO_ROOT_USER` e `MINIO_ROOT_PASSWORD` com valores locais próprios. Se já houver `.env` de outro projeto, preserve-o e forneça as variáveis no ambiente do terminal.
3. Execute `docker compose up --build`.
4. Acesse o frontend em `http://localhost:3000`, a saúde do backend em `http://localhost:8080/actuator/health`, o MailHog em `http://localhost:8025` e o console MinIO em `http://localhost:9001`.
5. No Compose, o backend usa o perfil `dev`: abra o portal e informe um nome e e-mail de teste. O usuário é criado como `FUNCIONARIO` e a sessão fica em cookie HttpOnly. Use dados fictícios nesse ambiente.

As portas do Compose ficam ligadas a `127.0.0.1`, pois o perfil `dev` permite simular qualquer e-mail. **Não use esse perfil em um ambiente acessível por outras pessoas.** A migration V1 cria o esquema PostgreSQL; o backend valida o mapeamento JPA na inicialização.

### Prévia visual sem Docker

Quando Docker ou o backend Java não estiverem disponíveis, a API simulada permite navegar pelas telas básicas. Ela escuta apenas em `127.0.0.1`, guarda dados somente na memória e inclui três chamados fictícios. A prévia cobre até E8; as funções E9–E11 exigem o backend real. Cálculo real de SLA em horas úteis, envio SMTP, autorização e persistência precisam do backend Java para verificação funcional. Não representa uma validação da integração com PostgreSQL, segurança ou SSO.

Em dois terminais PowerShell, dentro de `frontend/`:

```powershell
node scripts/preview-api.mjs
```

```powershell
$env:HELPDESK_DEV_API_TARGET = 'http://127.0.0.1:8188'
node node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5173 --strictPort
```

Abra `http://localhost:5173`. Na tela de login, use **Acessar prévia como agente de TI** para abrir a fila. Se já estiver em **Meus chamados** como funcionário, o mesmo botão aparece no início da página. Também é possível entrar com `agente@exemplo.local` ou `admin@exemplo.local` e qualquer nome; para a visão de funcionário, use `maria@exemplo.local`. Esses perfis são exclusivos da API simulada; o backend real continua provisionando FUNCIONARIO no primeiro acesso. Os chamados e o histórico criados nessa prévia desaparecem ao reiniciar a API simulada. Para validar o sistema real, use o Compose descrito acima.

## SSO em produção

Produção seleciona S3 obrigatoriamente e recusa storage local. As quatro variáveis `HELPDESK_S3_ENDPOINT`, `HELPDESK_S3_ACCESS_KEY`, `HELPDESK_S3_SECRET_KEY` e `HELPDESK_S3_BUCKET` devem ser não vazias; configure bucket privado antes de subir. A inicialização valida a configuração, sem testar a disponibilidade remota do bucket. Valide upload/download no teste operacional.

SMTP usa `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS_ENABLE` e `SMTP_STARTTLS_REQUIRED`. Em dev as credenciais podem estar vazias e as flags false para MailHog. Em prod as credenciais são obrigatórias e as três flags precisam ser true (padrão do perfil prod). Não reutilize as flags false do exemplo dev em produção. Há limites de tempo para conexão/leitura/escrita SMTP.

O nginx inclui CSP, nosniff, Referrer-Policy e Permissions-Policy. HSTS só é enviado quando o nginx recebe HTTPS; no Compose HTTP ele é omitido. Se o TLS terminar em outro proxy, configure HSTS nesse proxy e restrinja o nginx à rede interna. Não confie em um cabeçalho de protocolo enviado por clientes externos sem um proxy confiável.

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

Substitua o e-mail de exemplo pelo usuário aprovado. Após a primeira promoção, o `TI_ADMIN` administra perfis, usuários, categorias, SLA e calendário pela interface/API. Alterações de perfil ou desativação no banco passam a valer na próxima requisição da sessão existente.

## Builds independentes

- Backend: `cd backend && ./mvnw verify` (Windows: `mvnw.cmd verify`).
- Frontend: `cd frontend && npm ci && npm run lint && npm test && npm run build`.

## Banco de dados

O volume `postgres_data` guarda dados locais. Para backup em ambiente real, execute `pg_dump` em formato customizado e salve-o em destino protegido. Teste a restauração periodicamente em banco isolado com `pg_restore` antes de liberar escrita. Não use `docker compose down -v` em ambiente com dados a preservar.

## Saúde e diagnóstico

`/actuator/health` cobre liveness/readiness do processo. `/actuator/metrics` exige sessão TI_ADMIN e expõe, entre outras, `helpdesk.outbox.pending`, `helpdesk.sla.overdue` e `helpdesk.retention.storage.pending`. Configure alertas para valores persistentes acima de zero, acompanhando a tendência e os logs antes de agir. Em produção os logs saem em JSON ECS; `X-Request-ID` é devolvido ao cliente e registrado como `correlationId` para correlacionar requisições. Não registre conteúdo de chamados, anexos ou dados pessoais em logs. Restrinja o acesso de rede ao Actuator; a autorização da aplicação é uma segunda camada.

### Retenção de dados

`HELPDESK_RETENTION_YEARS` define de 1 a 30 anos (padrão 3) após o fechamento. `HELPDESK_RETENTION_ENABLED=false` suspende o job; use isso durante restauração e investigação. O job diário às 03:00 em America/Sao_Paulo processa até 10 lotes de 100 chamados FECHADOS por execução. Remove mensagens, histórico, avaliação e metadados de anexos; anonimiza título, descrição, solução, pessoas vinculadas e vínculos de duplicidade. O registro mínimo do chamado e seus agregados permanecem para estatística. Uma conta técnica inativa recebe a referência obrigatória de solicitante. A exclusão física dos objetos é registrada em `storage_exclusao_pendente` e repetida até concluir; acompanhe a métrica da fila. Notificações da outbox mais antigas que o prazo também são removidas. Defina o prazo com o encarregado de dados antes do primeiro deploy e ajuste os ciclos de backup para não reintroduzir dados já eliminados.

### Backup, restauração e implantação

1. Antes da atualização, registre a versão do código e do banco e confirme cópias íntegras de PostgreSQL (`pg_dump --format=custom`) e do bucket S3 privado. Faça o backup com criptografia, controle de acesso e janela de retenção própria.
2. Restaure periodicamente em ambiente isolado com `pg_restore --clean --if-exists --no-owner`, usando um banco vazio de teste. Compare contagem de chamados, anexos e versão Flyway; valide download de anexos e login com identidade de teste. Nunca restaure sobre a produção em operação.
3. Em uma atualização, habilite página de manutenção, suspenda o job de retenção se necessário, gere backup e publique primeiro a imagem do backend com as variáveis do ambiente. Flyway aplica as migrations versionadas na inicialização. Aguarde `/actuator/health/readiness` saudável e confira os logs com o identificador da implantação.
4. Publique o frontend da mesma revisão e faça um teste guiado: login, abertura, fila, comentário, anexo, resolução, relatório e logout. Retire a manutenção após confirmar as métricas e reative o job. Não é prometido deploy sem indisponibilidade.
5. Para voltar atrás, interrompa escrita, restaure conjuntamente banco e objetos do backup anterior e publique as imagens anteriores. Não edite uma migration já aplicada; adicione uma nova migration corretiva. Registre a janela de perda de dados entre backup e falha e comunique os responsáveis.

O pipeline executa testes, lint e build. O teste de PostgreSQL com Testcontainers inclui massa de 100 mil chamados e verifica p95 da fila abaixo de 300 ms no ambiente de CI. Esse valor depende do hardware e da carga; meça novamente no ambiente de destino antes de assumir a mesma latência em produção.

## Armazenamento de anexos

`HELPDESK_ATTACHMENT_TYPES` define as extensões habilitadas dentre as suportadas (padrão no `.env.example`). Tipos não suportados impedem inicialização; essa configuração não libera executáveis. A validação de ZIP/Office tem limites fixos de entradas/descompactação e rejeita macros identificáveis, caminhos perigosos e compactados aninhados. O scanner padrão aceita após a validação de formato; para antivírus, registre outra implementação `ScannerAnexo`. Não há integração antivírus nesta entrega.

Em dev, o backend grava em `./data/anexos` dentro do contêiner (ou em `HELPDESK_STORAGE_LOCAL_DIRECTORY` fora do Compose). Mantenha esse diretório fora da pasta pública do frontend. No Compose, o volume `attachments_data` persiste os arquivos; faça backup junto do PostgreSQL, preservando a consistência dos metadados.

Em produção, `application-prod.yml` exige S3 compatível. Crie um bucket privado antes de iniciar e configure `HELPDESK_S3_ENDPOINT`, `HELPDESK_S3_ACCESS_KEY`, `HELPDESK_S3_SECRET_KEY` e `HELPDESK_S3_BUCKET` no gerenciador de segredos. Não conceda leitura pública ou URLs permanentes. Valide upload e download com contas de funcionário, agente e chamado alheio antes de liberar o ambiente.

Na prévia sem Docker, comentários e metadados de anexos ficam em memória e o download devolve um arquivo demonstrativo; a validação real de conteúdo e a persistência são feitas pelo backend Java.

## Notificações, SLA e conclusão

No Compose, o MailHog recebe mensagens em `http://localhost:8025`. Configure `HELPDESK_MAIL_FROM` e `HELPDESK_PORTAL_URL` para o remetente e o endereço que aparecerá no e-mail; para o Compose local, o portal é `http://localhost:3000`. A outbox persiste tentativas e aplica espera crescente nas falhas. Se o envio já chegou ao SMTP e o processo caiu antes de confirmar a transação, o e-mail pode ser repetido; monitore a outbox e o MailHog ao testar o fluxo.

O SLA considera America/Sao_Paulo, janelas de expediente e feriados administrados na área da TI. O cálculo para um chamado novo usa a política vigente. Revise horário/feriados antes de colocar o sistema em operação; alterações não recalculam chamados já abertos. O job consulta prazos de resolução e de primeira resposta que vencem na próxima hora, excluindo concluídos, pausados e, para primeira resposta, chamados já respondidos. Cada prazo é deduplicado por chamado, tipo, instante e destinatário.

`HELPDESK_MAIL_TI_MODE=usuarios` mantém o envio à TI ativa. `lista` usa `HELPDESK_MAIL_TI_ADDRESSES`, com um ou mais endereços separados por vírgula; configuração vazia/inválida impede a inicialização. O mesmo destino recebe novos chamados, reaberturas, comentários do solicitante sem responsável e alertas sem responsável ativo. Chamados atribuídos notificam o responsável ativo. E-mails têm HTML escapado e alternativa em texto, status e link; a resolução inclui `#avaliacao`. Não incluem descrição, solução nem notas internas; alertas de SLA usam título genérico.

`HELPDESK_REOPEN_DAYS` (padrão 7) controla o limite de reabertura de um RESOLVIDO; `HELPDESK_AUTOCLOSE_DAYS` (padrão 3) controla o fechamento automático. O job roda a cada hora e processa até 100 chamados por execução. Para validar localmente, resolva um chamado, avalie com o solicitante e reabra; a avaliação anterior é apagada. Um chamado FECHADO permanece fechado.
# Indicadores de resolução

A V9 adiciona a medição incremental de minutos úteis na resolução. O calendário atual é aplicado pelo `CalendarioUtil`, incluindo a espera pelo solicitante; reabrir limpa a medição até a próxima resolução. Não há backfill de resoluções antigas porque o calendário histórico não foi versionado. O dashboard informa quantas não têm medição. Filtra por data de criação (últimos 30 dias por padrão), com máximo de 366 dias. Ver ADR 0008.
