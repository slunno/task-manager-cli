# ADR 0002 — Autenticação BFF com OIDC

**Status:** aceito em 2026-09-24; implementado na E1.

## Contexto

Funcionários usarão SSO corporativo. O navegador precisa acessar a API sem guardar tokens de identidade em JavaScript.

## Decisão

Spring Security `oauth2Login` executa o fluxo OIDC no perfil `prod`, com provedor configurado por variáveis de ambiente. O backend mantém a sessão no servidor e emite cookie `HttpOnly`, `Secure` e `SameSite=Lax` em produção. CSRF permanece ativo, inclusive no login simulado e no logout; o frontend obtém o token por `GET /api/v1/auth/csrf`. Frontend e backend são publicados na mesma origem via proxy, que encaminha também os caminhos de autorização e retorno OIDC.

No primeiro login, a identidade validada pelo provedor cria um usuário `FUNCIONARIO`, vinculado ao e-mail corporativo normalizado. `email_verified=false` e domínios diferentes do configurado são rejeitados. Perfis e estado ativo são consultados no banco em cada requisição autenticada, para que promoção e desativação tenham efeito na sessão existente. Um usuário inativo perde o acesso e sua sessão é invalidada.

O login local simulado existe somente com perfil `dev` e sem `prod`, é exposto apenas em localhost no Compose e usa o mesmo mecanismo de sessão/CSRF. Não há senha padrão nem conta administrativa embutida. O primeiro `TI_ADMIN` precisa ser promovido uma vez por um operador com acesso ao banco, depois do primeiro login SSO; o procedimento está no runbook.

## Consequências

- O cliente JavaScript não armazena token OIDC ou JWT próprio.
- Frontend e API devem compartilhar origem no deploy. Redirect URI e cabeçalhos de proxy precisam ser validados em HTTPS com o provedor real.
- O logout encerra a sessão local, sem prometer logout global do provedor.
- O modo `dev` nunca deve ser publicado para outros usuários, pois simula identidades por e-mail.
