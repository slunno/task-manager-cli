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
