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

Na E11, `GestaoIntegrationTest` cobre acesso administrativo a setores, associação de usuário e filtro de relatório, além de negar métricas ao funcionário. `RetencaoServiceTest` cobre anonimização e repetição da exclusão física de anexos. `CorrelationIdFilterTest` cobre validação e limpeza do identificador de requisição. `PostgresMigrationTest` mede o p95 da fila com 100 mil chamados e limite de 300 ms no runner de CI; a medição local depende de Docker.

## E3 — fila e operação

| Perfil | GET fila | POST assumir | PATCH chamado | GET histórico | GET busca de pessoas |
| --- | --- | --- | --- | --- | --- |
| Sem sessão | 401 | 401 com CSRF válido | 401 com CSRF válido | 401 | 401 |
| FUNCIONARIO | 403 | 403 | 403 | 403 | 403 |
| TI_AGENTE | 200 | 200 | 200 | 200 | 200 |
| TI_ADMIN | 200 | 200 | 200 | 200 | 200 |

`OperacaoTiIntegrationTest` verifica filtros, paginação, isolamento da fila, busca restrita à TI, CSRF, promoção de perfil durante a sessão, conflito de versão, impossibilidade de assumir chamado já atribuído, transições inválidas, solução obrigatória e o histórico gravado. `ChamadoTransicoesTest` exercita as regras do domínio. A versão do JPA é testada com duas cópias da mesma entidade.

## Conversa, anexos e conclusão

| Recurso | Funcionário dono | Funcionário alheio | TI agente/admin |
| --- | --- | --- | --- |
| Mensagens públicas/timeline | 200 | 404 | 200 |
| Listagem de comentários/anexos internos | Filtrados no banco | 404 | Incluídos |
| Download de anexo interno | 404 | 404 | 200 |
| Criar nota/anexo interno | 403 | 404 no acesso ao chamado | Permitido com CSRF em chamado ativo |
| Histórico completo | 403 | 403 | 200 |
| Reabrir/avaliar | Só o solicitante; status/prazo/version validados | 404 | Leitura da avaliação permitida; não cria em nome do solicitante |
| Mutação sem CSRF válido | 403 | 403 | 403 |

OperacaoTiIntegrationTest e ConclusaoIntegrationTest mantêm os controles de visibilidade, transições e titularidade. AnexosTiposIntegrationTest verifica upload/download real de cada formato novo e rejeição de conteúdo inválido; ConteudoAnexoTest cobre ZIP malicioso e scanner antes da gravação. ConfiguracaoProducaoTest cobre inicialização segura. NotificacaoSlaTest e SlaAlertasConsultaTest usam relógio fixo para prazos, pausa/resposta e deduplicação. DashboardServiceTest usa expediente/fim de semana/feriado e período controlados.

HistoricoPostgresTest confirma UPDATE/DELETE avulsos negados pelo banco, UPDATE também no contexto de retenção, rollback, anonimização/exclusão legítima e bloqueio após a desativação do contexto. PostgreSQL da CI executa os cinco testes de containers sem skips, incluindo Flyway V1–V10 e p95 da fila/dashboard com 100 mil chamados.

Os quatro testes Playwright em frontend/e2e/helpdesk.spec.ts percorrem o atendimento com sessões distintas de funcionário/TI/outro funcionário, na API real do Compose. Verificam também a resposta HTTP 404 sem conteúdo para o chamado, a timeline e os anexos de outra pessoa. A tela da avaliação é usada após uma segunda resolução, pois reabrir remove a conclusão anterior. HTML/texto no MailHog e download com attachment/nosniff têm verificação de integração no navegador. Dados, textos e destinatários são fictícios.

O gate JaCoCo mede linhas do conjunto domain/application (80% mínimo), sem afirmar cobertura de todos os ramos ou de cada pacote isoladamente. Relatórios e execução estão em [qualidade](qualidade.md); autenticação corporativa e serviços externos de produção ainda exigem validação no ambiente de destino.
