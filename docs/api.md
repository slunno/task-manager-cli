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

Corpo de criação: titulo (5–200 caracteres no formulário, máximo 200 no backend), descricao (máximo 10 mil caracteres no backend), categoriaId, prioridadeSugerida opcional. A prioridade final começa em MEDIA; a TI pode alterá-la. A API permite solicitanteId somente a agente/admin, para abrir em nome de outro usuário ativo. Funcionário não pode enviar esse campo, mesmo com o próprio ID.

O número CH-AAAA-NNNNNN usa sequência no banco e ano no fuso America/Sao_Paulo. A lista usa paginação obrigatória (page >= 0, 1 <= size <= 100) e aceita somente criadoEm ou numero com direção asc/desc. A resposta é { content, page, size, totalElements, totalPages }.

As consultas de funcionário aplicam solicitante_id no repositório, inclusive no detalhe. A tela não determina essa permissão. Respostas com dados de sessão/chamado usam Cache-Control: no-store.

## Operação da TI — E3

| Método | Caminho | Acesso | Resultado |
| --- | --- | --- | --- |
| GET | /api/v1/chamados | TI_AGENTE, TI_ADMIN | Fila paginada com filtros |
| POST | /api/v1/chamados/{id}/assumir | TI_AGENTE, TI_ADMIN + CSRF | Atribui ao agente logado; 409 se já atribuído, concluído ou versão divergente |
| PATCH | /api/v1/chamados/{id} | TI_AGENTE, TI_ADMIN + CSRF | Altera responsável, prioridade, categoria ou status |
| GET | /api/v1/chamados/{id}/historico | TI_AGENTE, TI_ADMIN | Histórico paginado, com alterações recentes primeiro |
| GET | /api/v1/usuarios/busca?texto=...&somenteTi=true | TI_AGENTE, TI_ADMIN | Até 20 usuários ativos com nome/e-mail correspondentes; somenteTi filtra agentes e administradores |

A fila aceita `status`, `prioridade`, `responsavelId`, `categoriaId`, `setorId`, `desde`, `ate`, `texto`, `semResponsavel`, `meus` e `slaVencendo`. As datas são dias locais de São Paulo, com limite final exclusivo. O último filtro só produzirá resultados quando E6 calcular `prazoResolucao`. A ordenação tem whitelist: `criadoEm`, `numero`, `atualizadoEm`, `prioridade`, `status` e `prazoResolucao`.

Assumir recebe `{ "version": N }`. PATCH recebe `version` e ao menos uma mudança: `status`, `prioridade`, `categoriaId`, `responsavelId`, `removerResponsavel` ou `solucao` ao resolver. A versão é verificada antes da alteração e o campo JPA `@Version` protege conflitos simultâneos. Cada mudança relevante grava histórico e publica `ChamadoAlteradoEvent` na transação. A resolução exige texto de solução; reabertura e fechamento ficam para E8.

A busca de pessoas exige termo de 2 a 100 caracteres, limita a 20 resultados e não é acessível ao funcionário. O cliente usa a busca para atribuir chamados e para abrir um chamado em nome de outro usuário. E4 adicionará comentários, notas internas e anexos.
