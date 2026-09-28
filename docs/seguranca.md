# Revisão de segurança E11

## C2 — Configuração de produção

Prod exige S3, credenciais SMTP e STARTTLS obrigatório; configuração ausente ou desativada interrompe a inicialização. Dev mantém MailHog sem autenticação e storage local. Nginx aplica CSP sem scripts inline, nosniff, Referrer-Policy e bloqueia câmera/microfone/geolocalização. A CSP admite estilos inline utilizados por componentes React, sem liberar scripts. HSTS é restrito a HTTPS e deve ser definido no proxy que termina TLS.

## Controles verificados

- Sessão no servidor, cookie HttpOnly/Secure/SameSite, CSRF nas mutações e OIDC corporativo no perfil `prod`; login simulado limitado ao perfil `dev`.
- Autorização no backend por perfil e titularidade; testes de integração cobrem funcionário, agente, administrador e acesso a chamado alheio. Métricas ficam restritas a TI_ADMIN.
- Entradas validadas, consultas parametrizadas, limites de paginação e tamanho de arquivos. Anexos são privados e o download repete a autorização do chamado.
- CSP e cabeçalhos de segurança no Spring Security. Conteúdo de chamados não entra nos logs operacionais.
- Contas inativas perdem acesso. A conta técnica de anonimização não pode ser editada nem importada por CSV.
- O identificador recebido em `X-Request-ID` só é reutilizado se corresponder a caracteres permitidos e até 64 posições; os demais são substituídos por UUID.

## Riscos e validação antes de produção

- Validar configuração do IdP, domínio autorizado, proxy HTTPS, política de sessão, bucket privado, SMTP e segredos no ambiente de destino.
- Testar anexos com arquivos de tipos permitidos e proibidos e revisar o antivírus corporativo na borda de entrada. O portal valida tipo, assinatura e limites de ZIP/Office; `ScannerAnexo` é um ponto de extensão com padrão sem varredura antivírus. Reduza tipos com `HELPDESK_ATTACHMENT_TYPES` quando necessário. Arquivos compactados aninhados são bloqueados, inclusive Office dentro de ZIP, por decisão conservadora.
- Definir retenção e janela de backup com o responsável por privacidade. Testar restauração isolada e remoção física de anexos.
- Executar verificação de dependências e teste de penetração no ambiente de homologação. A revisão de código não substitui esses controles operacionais.

## Integridade do histórico

Trigger da V10 rejeita UPDATE e DELETE avulsos. Exceção LGPD: contexto local da transação da retenção. O mecanismo previne alterações acidentais e não protege contra operadores com acesso SQL capaz de ativar o contexto ou desabilitar o trigger. Separe credenciais de migrations e aplicação e restrinja administração; ver ADR 0007.

## Destinos, conteúdo e configuração

Produção exige `HELPDESK_S3_ENDPOINT`, `HELPDESK_S3_ACCESS_KEY`, `HELPDESK_S3_SECRET_KEY`, `HELPDESK_S3_BUCKET`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH=true`, `SMTP_STARTTLS_ENABLE=true` e `SMTP_STARTTLS_REQUIRED=true`, além de banco e OIDC descritos no runbook. Endpoint/bucket S3 devem usar transporte seguro e ACL privada no ambiente de destino. Valores pertencem ao gerenciador de segredos; o exemplo contém somente configurações de dev.

`HELPDESK_MAIL_TI_MODE` aceita `usuarios` ou `lista`; a lista validada fica em `HELPDESK_MAIL_TI_ADDRESSES`. A configuração define quem recebe informação operacional e deve passar por revisão do dono. Templates escapam conteúdo dinâmico e têm alternativa de texto; descrição, solução e notas internas não são incluídas. Alertas SLA usam título genérico. Não há garantia de entrega exatamente uma vez após falha entre SMTP e confirmação da outbox.

`HELPDESK_ATTACHMENT_TYPES` só habilita formatos suportados. A proteção de ZIP limita descompactação/entradas e bloqueia conteúdo executável identificável, macros e arquivos compactados dentro de compactados; não é uma garantia de ausência de malware. O scanner padrão é uma extensão sem antivírus. Conteúdo é sempre baixado como attachment/nosniff.

## Evidência automatizada

Testes de produção usam a configuração real e verificam S3 obrigatório, ausência de fallback e SMTP seguro. E2E verifica CSP do build nginx, ausência de erro JavaScript, CSRF, download e isolamento de titularidade/notas. Testes PostgreSQL verificam imutabilidade e exceção de retenção. [Matriz de autorização](testes-autorizacao.md) e [qualidade](qualidade.md) registram cenários e limites.
