# ADR 0002 — Autenticação BFF com OIDC

**Status:** aceito em 2026-09-24

## Contexto

Funcionários usarão SSO corporativo. O navegador precisa acessar a API sem guardar tokens de identidade em JavaScript.

## Decisão

Spring Security `oauth2Login` fará o fluxo OIDC com provedor configurado por variáveis de ambiente. A aplicação manterá sessão no servidor e emitirá cookie `HttpOnly`, `Secure` e `SameSite` adequado ao mesmo domínio do front. CSRF permanece ativo, inclusive para chamadas mutáveis. O front obterá estado da sessão em `GET /api/v1/me`.

No primeiro login, a identidade validada pelo provedor cria um usuário `FUNCIONARIO`. A promoção para perfis de TI é ato de `TI_ADMIN`. Um usuário inativo não estabelece sessão utilizável. Um login local simulado existirá somente no perfil `dev`, protegido contra ativação em produção.

## Consequências

- Front e API devem ser servidos sob a mesma origem no deploy, por proxy reverso.
- Não haverá JWT próprio nem token persistido no navegador.
- Configuração do provedor e cookies deve ser testada em ambiente com HTTPS antes de produção.

