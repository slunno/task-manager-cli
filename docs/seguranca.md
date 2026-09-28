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
- Testar anexos com arquivos de tipos permitidos e proibidos e revisar o antivírus corporativo na borda de entrada. O portal valida tipo e assinatura, mas não executa varredura antivírus.
- Definir retenção e janela de backup com o responsável por privacidade. Testar restauração isolada e remoção física de anexos.
- Executar verificação de dependências e teste de penetração no ambiente de homologação. A revisão de código não substitui esses controles operacionais.
