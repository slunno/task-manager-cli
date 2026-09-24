# Matriz de autorização automatizada

Os testes de integração em ChamadoAutorizacaoIntegrationTest exercitam a API real com Spring Security, sessão de desenvolvimento e CSRF. O filtro de sessão recarrega o perfil e o estado ativo do banco em cada requisição. A coluna de detalhe para funcionário alheio deve permanecer 404, inclusive quando o ID existe, para não revelar a existência do recurso.

| Perfil e recurso | GET categorias | POST chamados | GET chamados/meus | GET chamados/{id} |
| --- | --- | --- | --- | --- |
| Sem sessão | 401 | 401 após CSRF válido; sem CSRF: 403 | 401 | 401 |
| FUNCIONARIO, próprio | 200 | 201, solicitante da sessão | 200, só os próprios | 200 |
| FUNCIONARIO, alheio | 200 | campo solicitanteId rejeitado com 400 | não aparece | 404 |
| TI_AGENTE, alheio | 200 | 201 em nome de usuário ativo | lista apenas chamados em que é solicitante | 200 |
| TI_ADMIN, alheio | 200 | 201 em nome de usuário ativo | lista apenas chamados em que é solicitante | 200 |

Os métodos públicos do controller exigem FUNCIONARIO, TI_AGENTE ou TI_ADMIN por @PreAuthorize. A aplicação filtra a lista por solicitante_id na consulta findBySolicitanteId e o detalhe por findByIdAndSolicitanteId quando o perfil é FUNCIONARIO. Não há consulta ampla seguida de filtro em memória.

Testes adicionais cobrem categoria ativa, validação do formulário, limite de página, whitelist de ordenação, prioridade final MEDIA, número legível único, perfil alterado durante a sessão, usuário inativo e CSRF. As fronteiras entre módulos são verificadas por FronteirasModularesTest (ArchUnit). PostgresMigrationTest valida as migrations e o mapeamento JPA em PostgreSQL 16 quando Docker está disponível; é ignorado na máquina local sem Docker e executado na CI com Docker.

## E3 — fila e operação

| Perfil | GET fila | POST assumir | PATCH chamado | GET histórico |
| --- | --- | --- | --- | --- |
| Sem sessão | 401 | 401 com CSRF válido | 401 com CSRF válido | 401 |
| FUNCIONARIO | 403 | 403 | 403 | 403 |
| TI_AGENTE | 200 | 200 | 200 | 200 |
| TI_ADMIN | 200 | 200 | 200 | 200 |

`OperacaoTiIntegrationTest` verifica filtros, paginação, isolamento da fila, CSRF, promoção de perfil durante a sessão, conflito de versão, impossibilidade de assumir chamado já atribuído, transições inválidas, solução obrigatória e o histórico gravado. `ChamadoTransicoesTest` exercita as regras do domínio. A versão do JPA é testada com duas cópias da mesma entidade.
