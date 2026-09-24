# API HTTP

Prefixo `/api/v1`, JSON, erros no formato Problem Details. No perfil `dev`, após autenticação, o contrato OpenAPI fica em `/v3/api-docs` e a UI em `/swagger-ui/index.html`; ambos ficam desligados em `prod`.

## Identidade — E1

| Método | Caminho | Acesso | Resposta |
| --- | --- | --- | --- |
| GET | `/api/v1/auth/config` | público | `{ "modo": "dev" \| "oidc" \| "disabled", "urlLogin": string \| null }` |
| GET | `/api/v1/auth/csrf` | público | `{ "token": string }`; usar em `X-CSRF-TOKEN` para mutações |
| POST | `/api/v1/auth/dev/login` | somente perfil `dev` | Corpo `{ "email", "nome" }`; cria sessão e retorna usuário |
| GET | `/api/v1/me` | autenticado e ativo | `{ "id", "nome", "email", "perfil" }` |
| POST | `/api/v1/auth/logout` | sessão | `204 No Content`; invalida sessão |

No perfil `prod`, o navegador inicia o login por `GET /oauth2/authorization/corporativo`, e o provedor retorna a `/login/oauth2/code/corporativo`. Essas rotas pertencem ao Spring Security, fora do prefixo da API.

O frontend sempre envia cookies da mesma origem. Um `401` em `/me` representa ausência de sessão; `403` em mutações sem token CSRF ou com token inválido. As respostas com dados de sessão usam `Cache-Control: no-store`.

## Evolução

E2 adicionará `GET /categorias`, `POST /chamados`, `GET /chamados/meus` e `GET /chamados/{id}`. Os contratos de negócio terão paginação no servidor, ordenação permitida explicitamente, validação Bean Validation e autorização no backend. O cliente TypeScript gerado do OpenAPI entra quando o contrato de chamados estiver definido; as chamadas de bootstrap de autenticação são tipadas manualmente neste incremento.
