# Revisão de segurança E11

## C2 — Configuração de produção

Prod exige S3, credenciais SMTP e STARTTLS obrigatório; configuração ausente ou desativada interrompe a inicialização. Dev mantém MailHog sem autenticação e storage local. Nginx aplica CSP sem scripts ou estilos inline, nosniff, Referrer-Policy e bloqueia câmera/microfone/geolocalização. HSTS é restrito a HTTPS e deve ser definido no proxy que termina TLS.

## Segunda rodada — proxy, contêineres e CSP

O proxy publica somente health do Actuator; métricas e documentação retornam 404, mesmo quando o backend dev as disponibiliza. Backend e frontend executam sem root, com capabilities removidas e sem ganho de privilégios no Compose. As sondas HTTP verificam a aplicação e o proxy; dependências aguardam saúde antes de iniciar.

A inspeção de `frontend/src` não encontrou atributos `style`, blocos de estilo inline ou alterações de `element.style`; Tailwind gera um arquivo CSS externo no build. O componente Radix usado atualmente é Slot, sem necessidade de posicionamento inline. Por isso `style-src` foi reduzido a `'self'`. Playwright verifica o cabeçalho e falha se encontrar erro de CSP durante o atendimento. Novos componentes com posicionamento dinâmico precisam de nova avaliação desta decisão antes de ampliar a política; não se libera script inline.

## Controles verificados

- Sessão no servidor, cookie HttpOnly/Secure/SameSite, CSRF nas mutações e OIDC corporativo no perfil `prod`; login simulado limitado ao perfil `dev`.
- Autorização no backend por perfil e titularidade; testes de integração cobrem funcionário, agente, administrador e acesso a chamado alheio. Métricas ficam restritas a TI_ADMIN.
- Entradas validadas, consultas parametrizadas, limites de paginação e tamanho de arquivos. Anexos são privados e o download repete a autorização do chamado.
- CSP e cabeçalhos de segurança no Spring Security. Conteúdo de chamados não entra nos logs operacionais.
- Contas inativas perdem acesso. A conta técnica de anonimização não pode ser editada nem importada por CSV.
- O identificador recebido em `X-Request-ID` só é reutilizado se corresponder a caracteres permitidos e até 64 posições; os demais são substituídos por UUID.

## Riscos e validação antes de produção

### Limitação de taxa e proxies

Janela fixa em memória, separada por operação: login (incluindo senha/dev e início/callback OIDC) usa IP; criação de chamado, comentário e anexo usam identidade autenticada, ou IP se anônimo. Valores por minuto: 30 logins, 30 chamados, 60 comentários, 30 uploads. Configuráveis por `HELPDESK_RATE_LIMIT_*`; recusa com 429 Problem Details e `Retry-After`. O armazenamento é limitado a 10.000 chaves e remove entradas expiradas. Nenhum IP, identidade ou corpo é registrado pelo limitador. Não há biblioteca nova.

O limite é por instância e reinicia com o processo; não constitui proteção distribuída contra DDoS. Até duas cotas podem ser usadas na fronteira de janelas fixas. O perfil `test` desabilita o filtro; testes dedicados verificam concorrência, isolamento e expiração. Em múltiplas instâncias, adotar proteção de borda/limite compartilhado é backlog.

Tomcat trata forwarded headers somente se o IP do par corresponder a `HELPDESK_TRUSTED_PROXY_REGEX` (vazio por padrão). Não use regex abrangente. O nginx substitui `X-Forwarded-For` pelo IP do cliente e remove `Forwarded`; uma cadeia com outro terminador TLS requer configuração explícita de confiança e cabeçalhos em cada salto. Sem confiança configurada, usuários anônimos atrás do nginx compartilham a cota do proxy; usuários autenticados continuam separados. O backend deve ser inacessível diretamente fora da rede autorizada. O redirecionamento OIDC de produção permanece uma URL absoluta configurada.

- Validar configuração do IdP, domínio autorizado, proxy HTTPS, política de sessão, bucket privado, SMTP e segredos no ambiente de destino.
- Testar anexos com arquivos de tipos permitidos e proibidos e revisar o antivírus corporativo na borda de entrada. O portal valida tipo, assinatura e limites de ZIP/Office; `ScannerAnexo` é um ponto de extensão com padrão sem varredura antivírus. Reduza tipos com `HELPDESK_ATTACHMENT_TYPES` quando necessário. Nomes/extensões de compactados aninhados conhecidos são bloqueados, inclusive Office dentro de ZIP, por decisão conservadora. Arquivos renomeados podem escapar dessa identificação e exigem scanner externo.
- Definir retenção e janela de backup com o responsável por privacidade. Testar restauração isolada e remoção física de anexos.
- Executar verificação de dependências e teste de penetração no ambiente de homologação. A revisão de código não substitui esses controles operacionais.

## Integridade do histórico

Trigger da V10 rejeita UPDATE e DELETE avulsos. Exceção LGPD: contexto local da transação da retenção. O mecanismo previne alterações acidentais e não protege contra operadores com acesso SQL capaz de ativar o contexto ou desabilitar o trigger. Separe credenciais de migrations e aplicação e restrinja administração; ver ADR 0007.

## Destinos, conteúdo e configuração

Produção exige `HELPDESK_S3_ENDPOINT`, `HELPDESK_S3_ACCESS_KEY`, `HELPDESK_S3_SECRET_KEY`, `HELPDESK_S3_BUCKET`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH=true`, `SMTP_STARTTLS_ENABLE=true` e `SMTP_STARTTLS_REQUIRED=true`, além de banco e OIDC descritos no runbook. Endpoint/bucket S3 devem usar transporte seguro e ACL privada no ambiente de destino. Valores pertencem ao gerenciador de segredos; o exemplo contém somente configurações de dev.

`HELPDESK_MAIL_TI_MODE` aceita `usuarios` ou `lista`; a lista validada fica em `HELPDESK_MAIL_TI_ADDRESSES`. A configuração define quem recebe informação operacional e deve passar por revisão do dono. Templates escapam conteúdo dinâmico e têm alternativa de texto; descrição, solução e notas internas não são incluídas. Alertas SLA usam título genérico. Não há garantia de entrega exatamente uma vez após falha entre SMTP e confirmação da outbox.

`HELPDESK_ATTACHMENT_TYPES` só habilita formatos suportados. A proteção de ZIP limita descompactação/entradas e bloqueia nomes/extensões de executáveis e compactados conhecidos e macros identificáveis; não é uma garantia de ausência de malware. O scanner padrão é uma extensão sem antivírus. Conteúdo é sempre baixado como attachment/nosniff.

## Evidência automatizada

Testes de produção usam a configuração real e verificam S3 obrigatório, ausência de fallback e SMTP seguro. E2E verifica CSP do build nginx, ausência de erro JavaScript, CSRF, download e isolamento de titularidade/notas. Testes PostgreSQL verificam imutabilidade e exceção de retenção. [Matriz de autorização](testes-autorizacao.md) e [qualidade](qualidade.md) registram cenários e limites.
