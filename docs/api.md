# API HTTP

Prefixo /api/v1, JSON, erros no formato Problem Details. No perfil dev, após autenticação, o contrato OpenAPI fica em /v3/api-docs e a UI em /swagger-ui/index.html; ambos ficam desligados em prod.

O snapshot [openapi.json](../frontend/openapi.json) é gerado por OpenApiSnapshotTest e comparado com os controllers na CI. O frontend usa openapi-typescript e openapi-fetch para as rotas de chamados. Após mudar o contrato, rode cd backend && ./mvnw -Dtest=OpenApiSnapshotTest test, copie backend/target/openapi.json para frontend/openapi.json e execute cd frontend && npm run api:generate.

## Identidade — E1

| Método | Caminho | Acesso | Resposta |
| --- | --- | --- | --- |
| GET | /api/v1/auth/config | público | { \"modo\": \"dev\" &#124; \"oidc\" &#124; \"disabled\", \"urlLogin\": string &#124; null } |
| GET | /api/v1/auth/csrf | público | { \"token\": string }; usar em X-CSRF-TOKEN para mutações |
| POST | /api/v1/auth/dev/login | somente perfil dev | Corpo { \"email\", \"nome\" }; cria sessão e retorna usuário |
| GET | /api/v1/me | autenticado e ativo | { \"id\", \"nome\", \"email\", \"perfil\" } |
| POST | /api/v1/auth/logout | sessão | 204 No Content; invalida sessão |

No perfil prod, o navegador inicia o login por GET /oauth2/authorization/corporativo, e o provedor retorna a /login/oauth2/code/corporativo. Essas rotas pertencem ao Spring Security, fora do prefixo da API. Um 401 em /me representa ausência de sessão; 403 em mutações sem token CSRF ou com token inválido.

## Chamados núcleo — E2

| Método | Caminho | Acesso | Resultado |
| --- | --- | --- | --- |
| GET | /api/v1/categorias | usuário ativo | Lista de categorias ativas, por nome |
| POST | /api/v1/chamados | usuário ativo + CSRF | 201 Created, Location e ChamadoResponse |
| GET | /api/v1/chamados/meus?page=0&size=20&sort=criadoEm,desc | usuário ativo | Página somente dos chamados cujo solicitante é a sessão |
| GET | /api/v1/chamados/{id} | funcionário dono ou TI | ChamadoResponse; 404 para funcionário em chamado alheio |

Corpo de criação: titulo (5–200 caracteres no formulário, máximo 200 no backend), descricao (máximo 10 mil caracteres no backend), categoriaId, prioridadeSugerida opcional. A prioridade final começa em MEDIA; a TI poderá alterá-la na E3. A API permite solicitanteId somente a agente/admin, para abrir em nome de outro usuário ativo. Funcionário não pode enviar esse campo, mesmo com o próprio ID.

O número CH-AAAA-NNNNNN usa sequência no banco e ano no fuso America/Sao_Paulo. A lista usa paginação obrigatória (page >= 0, 1 <= size <= 100) e aceita somente criadoEm ou numero com direção asc/desc. A resposta é { content, page, size, totalElements, totalPages }.

As consultas de funcionário aplicam solicitante_id no repositório, inclusive no detalhe. A tela não determina essa permissão. Respostas com dados de sessão/chamado usam Cache-Control: no-store.

## Evolução

E3 adicionará fila da TI, filtros, atribuição, transições de status e histórico. E4 adicionará comentários, notas internas e anexos. O contrato e o cliente gerado serão atualizados em cada etapa.
