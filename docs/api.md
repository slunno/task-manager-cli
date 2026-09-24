# API HTTP

A API versionada terá prefixo `/api/v1`. As rotas de negócio serão implementadas a partir de E1, e o contrato OpenAPI será gerado pelo springdoc em `/v3/api-docs`. Na E0, apenas o endpoint operacional `GET /actuator/health` está acessível sem autenticação. Qualquer outra rota exige sessão e, ainda que autenticada, não possui implementação de negócio.

Padrões definidos: JSON, paginação obrigatória nas listagens, campos de ordenação permitidos explicitamente, validação Bean Validation e erros em RFC 7807. O cliente TypeScript será gerado do OpenAPI quando o contrato de E1/E2 existir.
